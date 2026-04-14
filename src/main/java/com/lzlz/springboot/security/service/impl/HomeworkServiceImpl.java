package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lzlz.springboot.security.constants.RedisKeys;
import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.dto.StudentHomeworkDetailDto;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.entity.HomeworkSubmission;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.HomeworkMapper;
import com.lzlz.springboot.security.mapper.HomeworkSubmissionMapper;
import com.lzlz.springboot.security.service.HomeworkService;
import com.lzlz.springboot.security.service.MinIOService;
import com.lzlz.springboot.security.service.RedisCacheService;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class HomeworkServiceImpl implements HomeworkService {

    @Autowired
    private HomeworkMapper homeworkMapper;

    @Autowired
    private HomeworkSubmissionMapper submissionMapper;

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private StudentCourseAccessService studentCourseAccessService;

    @Value("${cache.ttl.homework-list-seconds:300}")
    private long homeworkListTtlSeconds;

    @Value("${cache.ttl.homework-detail-teacher-seconds:300}")
    private long homeworkTeacherDetailTtlSeconds;

    private void validateRelation(Long courseId, Long homeworkId, Long submissionId) {
        if (submissionId != null) {
            HomeworkSubmission sub = submissionMapper.selectById(submissionId);
            if (sub == null) {
                throw new ResourceNotFoundException("Homework submission not found: " + submissionId);
            }
            if (homeworkId != null && !sub.getHomeworkId().equals(homeworkId)) {
                throw new CustomGraphException(400, "Submission does not belong to homework");
            }
            homeworkId = sub.getHomeworkId();
        }

        if (homeworkId != null) {
            Homework hw = homeworkMapper.selectById(homeworkId);
            if (hw == null) {
                throw new ResourceNotFoundException("Homework not found: " + homeworkId);
            }
            if (courseId != null && !hw.getCourseId().equals(courseId)) {
                throw new CustomGraphException(400, "Homework does not belong to current course");
            }
        }
    }

    private String uploadHomeworkAttachments(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return "";
        }

        StringBuilder objectNames = new StringBuilder();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                String name = minIOService.uploadFile(file);
                if (objectNames.length() > 0) {
                    objectNames.append(",");
                }
                objectNames.append(name);
            } catch (Exception e) {
                throw new RuntimeException("Homework attachment upload failed: " + file.getOriginalFilename(), e);
            }
        }
        return objectNames.toString();
    }

    private String mergeAttachmentObjectNames(String fromRequest, String fromUploadedFiles) {
        if ((fromRequest == null || fromRequest.isBlank()) && (fromUploadedFiles == null || fromUploadedFiles.isBlank())) {
            return "";
        }
        if (fromRequest == null || fromRequest.isBlank()) {
            return fromUploadedFiles;
        }
        if (fromUploadedFiles == null || fromUploadedFiles.isBlank()) {
            return fromRequest;
        }
        return fromRequest + "," + fromUploadedFiles;
    }

    private boolean allowLate(Integer allowLateSubmit) {
        return allowLateSubmit != null && allowLateSubmit == 1;
    }

    private List<String> toSignedUrls(String objectNames) {
        if (objectNames == null || objectNames.isBlank()) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        for (String name : objectNames.split(",")) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            try {
                urls.add(minIOService.getPresignedUrl(name.trim()));
            } catch (Exception ignored) {
            }
        }
        return urls;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createHomework(Long courseId, CreateHomeworkRequest request, MultipartFile[] files) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : now;
        LocalDateTime endTime = request.getEndTime();

        if (endTime == null) {
            throw new CustomGraphException(400, "endTime is required");
        }
        if (endTime.isBefore(startTime)) {
            throw new CustomGraphException(400, "endTime must be later than startTime");
        }

        String uploadedAttachmentObjectNames = uploadHomeworkAttachments(files);
        String attachmentObjectNames = mergeAttachmentObjectNames(request.getAttachmentUrls(), uploadedAttachmentObjectNames);

        Homework homework = new Homework();
        homework.setCourseId(courseId);
        homework.setTitle(request.getTitle());
        homework.setContent((request.getContent() == null || request.getContent().isBlank()) ? request.getDescription() : request.getContent());
        homework.setStartTime(startTime);
        homework.setEndTime(endTime);
        homework.setAllowLateSubmit(Boolean.TRUE.equals(request.getAllowLateSubmit()) ? 1 : 0);
        homework.setTotalScore(request.getTotalScore() == null ? 100 : request.getTotalScore());
        homework.setAttachmentUrls(attachmentObjectNames);
        homework.setStatus(1);
        homeworkMapper.insert(homework);

        redisCacheService.delete(RedisKeys.homeworkList(courseId));
        redisCacheService.deleteByPrefix("homework:detail:teacher:" + courseId + ":");
    }

    @Override
    public List<Homework> getHomeworkList(Long courseId) {
        String key = RedisKeys.homeworkList(courseId);
        List<Homework> cached = redisCacheService.get(key, new TypeReference<List<Homework>>() {
        });
        if (cached != null) {
            return cached;
        }

        QueryWrapper<Homework> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.orderByDesc("created_at");

        List<Homework> result = homeworkMapper.selectList(wrapper);
        redisCacheService.set(key, result, Duration.ofSeconds(homeworkListTtlSeconds));
        return result;
    }

    @Override
    public List<StudentHomeworkDetailDto> getHomeworkListForStudent(Long courseId, Long studentId) {
        studentCourseAccessService.checkCourseAccess(studentId.intValue(), courseId);

        QueryWrapper<Homework> homeworkWrapper = new QueryWrapper<>();
        homeworkWrapper.eq("course_id", courseId);
        homeworkWrapper.orderByDesc("created_at");
        List<Homework> homeworkList = homeworkMapper.selectList(homeworkWrapper);

        LocalDateTime now = LocalDateTime.now();
        List<StudentHomeworkDetailDto> resultList = new ArrayList<>();

        for (Homework hw : homeworkList) {
            StudentHomeworkDetailDto dto = new StudentHomeworkDetailDto();
            dto.setId(hw.getId());
            dto.setTitle(hw.getTitle());
            dto.setContent(hw.getContent());
            dto.setStartTime(hw.getStartTime());
            dto.setEndTime(hw.getEndTime());
            dto.setAllowLateSubmit(allowLate(hw.getAllowLateSubmit()));
            dto.setTotalScore(hw.getTotalScore());
            dto.setLateWindow(hw.getEndTime() != null && now.isAfter(hw.getEndTime()));

            QueryWrapper<HomeworkSubmission> subWrapper = new QueryWrapper<>();
            subWrapper.eq("homework_id", hw.getId());
            subWrapper.eq("student_id", studentId);
            HomeworkSubmission sub = submissionMapper.selectOne(subWrapper);

            if (sub != null) {
                dto.setSubmitted(true);
                dto.setStatus(sub.getStatus());
            } else {
                dto.setSubmitted(false);
                dto.setStatus(0);
            }

            resultList.add(dto);
        }

        return resultList;
    }

    @Override
    public HomeworkDetailResponse getHomeworkDetailForTeacher(Long courseId, Long homeworkId) {
        String key = RedisKeys.homeworkTeacherDetail(courseId, homeworkId);
        HomeworkDetailResponse cached = redisCacheService.get(key, HomeworkDetailResponse.class);
        if (cached != null) {
            return cached;
        }

        validateRelation(courseId, homeworkId, null);
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new ResourceNotFoundException("Homework not found with id: " + homeworkId);
        }

        HomeworkDetailResponse response = new HomeworkDetailResponse();
        response.setId(homework.getId());
        response.setTitle(homework.getTitle());
        response.setContent(homework.getContent());
        response.setStartTime(homework.getStartTime());
        response.setEndTime(homework.getEndTime());
        response.setAllowLateSubmit(allowLate(homework.getAllowLateSubmit()));
        response.setTotalScore(homework.getTotalScore());
        response.setStatus(homework.getStatus());
        response.setAttachmentUrls(toSignedUrls(homework.getAttachmentUrls()));

        redisCacheService.set(key, response, Duration.ofSeconds(homeworkTeacherDetailTtlSeconds));
        return response;
    }

    @Override
    public StudentHomeworkDetailDto getHomeworkDetailForStudent(Long courseId, Long homeworkId, Long studentId) {
        studentCourseAccessService.checkHomeworkAccess(studentId.intValue(), courseId, homeworkId);
        validateRelation(courseId, homeworkId, null);

        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new ResourceNotFoundException("Homework not found: " + homeworkId);
        }

        StudentHomeworkDetailDto dto = new StudentHomeworkDetailDto();
        dto.setId(homework.getId());
        dto.setTitle(homework.getTitle());
        dto.setContent(homework.getContent());
        dto.setStartTime(homework.getStartTime());
        dto.setEndTime(homework.getEndTime());
        dto.setAllowLateSubmit(allowLate(homework.getAllowLateSubmit()));
        dto.setTotalScore(homework.getTotalScore());
        dto.setLateWindow(homework.getEndTime() != null && LocalDateTime.now().isAfter(homework.getEndTime()));
        dto.setHomeworkAttachmentUrls(toSignedUrls(homework.getAttachmentUrls()));

        QueryWrapper<HomeworkSubmission> subWrapper = new QueryWrapper<>();
        subWrapper.eq("homework_id", homeworkId);
        subWrapper.eq("student_id", studentId);
        HomeworkSubmission sub = submissionMapper.selectOne(subWrapper);

        if (sub != null) {
            dto.setSubmitted(true);
            dto.setStatus(sub.getStatus());
            dto.setFinalScore(sub.getFinalScore());
            dto.setTeacherComment(sub.getTeacherComment());
            dto.setAttachmentUrls(toSignedUrls(sub.getAttachmentUrls()));
        } else {
            dto.setSubmitted(false);
            dto.setStatus(0);
        }

        return dto;
    }
}
