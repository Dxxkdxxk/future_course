package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.StudentSubmissionDto;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.entity.HomeworkSubmission;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.HomeworkMapper;
import com.lzlz.springboot.security.mapper.HomeworkSubmissionMapper;
import com.lzlz.springboot.security.mapper.UserMapper;
import com.lzlz.springboot.security.service.HomeworkSubmissionService;
import com.lzlz.springboot.security.service.MinIOService;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HomeworkSubmissionServiceImpl implements HomeworkSubmissionService {

    @Autowired
    private HomeworkSubmissionMapper submissionMapper;

    @Autowired
    private HomeworkMapper homeworkMapper;

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentCourseAccessService studentCourseAccessService;

    private void validateRelation(Long courseId, Long homeworkId, Long submissionId) {
        if (submissionId != null) {
            HomeworkSubmission sub = submissionMapper.selectById(submissionId);
            if (sub == null) {
                throw new ResourceNotFoundException("Submission not found: " + submissionId);
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

    private boolean allowLate(Homework homework) {
        return homework.getAllowLateSubmit() != null && homework.getAllowLateSubmit() == 1;
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
            } catch (Exception e) {
                urls.add("Error: " + name);
            }
        }
        return urls;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitHomework(Long courseId, Long homeworkId, int studentId, MultipartFile[] files, String content) {
        studentCourseAccessService.checkHomeworkAccess(studentId, courseId, homeworkId);
        validateRelation(courseId, homeworkId, null);

        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new ResourceNotFoundException("Homework not found: " + homeworkId);
        }

        LocalDateTime now = LocalDateTime.now();
        if (homework.getStartTime() != null && now.isBefore(homework.getStartTime())) {
            throw new CustomGraphException(400, "Homework has not started yet");
        }
        if (homework.getEndTime() != null && now.isAfter(homework.getEndTime()) && !allowLate(homework)) {
            throw new CustomGraphException(400, "Homework deadline has passed and late submission is disabled");
        }

        StringBuilder objectNames = new StringBuilder();
        if (files != null && files.length > 0) {
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
                    log.error("MinIO upload failed", e);
                    throw new RuntimeException("File upload failed: " + file.getOriginalFilename());
                }
            }
        }

        String finalAttachmentUrls = objectNames.toString();
        HomeworkSubmission existing = getMySubmission(homeworkId, studentId);
        if (existing != null) {
            if (!finalAttachmentUrls.isEmpty()) {
                existing.setAttachmentUrls(finalAttachmentUrls);
            }
            existing.setSubmittedAt(now);
            existing.setStatus(1);
            submissionMapper.updateById(existing);
        } else {
            HomeworkSubmission submission = new HomeworkSubmission();
            submission.setHomeworkId(homeworkId);
            submission.setStudentId(studentId);
            submission.setAttachmentUrls(finalAttachmentUrls);
            submission.setSubmittedAt(now);
            submission.setStatus(1);
            submissionMapper.insert(submission);
        }
    }

    @Override
    public HomeworkSubmission getMySubmission(Long homeworkId, int studentId) {
        QueryWrapper<HomeworkSubmission> wrapper = new QueryWrapper<>();
        wrapper.eq("homework_id", homeworkId);
        wrapper.eq("student_id", studentId);
        return submissionMapper.selectOne(wrapper);
    }

    @Override
    public List<StudentSubmissionDto> getHomeworkSubmissionList(Long courseId, Long homeworkId) {
        validateRelation(courseId, homeworkId, null);

        QueryWrapper<HomeworkSubmission> wrapper = new QueryWrapper<>();
        wrapper.eq("homework_id", homeworkId);
        wrapper.orderByDesc("submitted_at");
        List<HomeworkSubmission> list = submissionMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> studentIds = list.stream().map(HomeworkSubmission::getStudentId).distinct().collect(Collectors.toList());
        List<User> students = userMapper.selectBatchIds(studentIds);
        Map<Integer, User> studentMap = students.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<StudentSubmissionDto> dtos = new ArrayList<>();
        for (HomeworkSubmission sub : list) {
            StudentSubmissionDto dto = new StudentSubmissionDto();
            dto.setSubmissionId(sub.getId());
            dto.setStudentId(sub.getStudentId());
            dto.setStatus(sub.getStatus());
            dto.setFinalScore(sub.getFinalScore());
            dto.setTeacherComment(sub.getTeacherComment());
            dto.setSubmittedAt(sub.getSubmittedAt());
            dto.setGradedAt(sub.getGradedAt());

            User u = studentMap.get(sub.getStudentId());
            if (u != null) {
                dto.setStudentName(u.getUsername());
                dto.setStudentEmail(u.getEmail());
            }

            dto.setAttachmentUrls(toSignedUrls(sub.getAttachmentUrls()));
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeSubmission(Long courseId, Long submissionId, Integer finalScore, String teacherComment) {
        validateRelation(courseId, null, submissionId);

        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new ResourceNotFoundException("Submission not found: " + submissionId);
        }

        Homework homework = homeworkMapper.selectById(submission.getHomeworkId());
        if (homework != null && homework.getTotalScore() != null && finalScore != null && finalScore > homework.getTotalScore()) {
            throw new CustomGraphException(400, "finalScore cannot exceed homework totalScore");
        }

        submission.setFinalScore(finalScore);
        submission.setTeacherComment(teacherComment);
        submission.setStatus(2);
        submission.setGradedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);
    }

    @Override
    public StudentSubmissionDto getSubmissionDetail(Long courseId, Long submissionId) {
        validateRelation(courseId, null, submissionId);

        HomeworkSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) {
            throw new ResourceNotFoundException("Submission not found with id: " + submissionId);
        }

        StudentSubmissionDto dto = new StudentSubmissionDto();
        dto.setSubmissionId(sub.getId());
        dto.setStudentId(sub.getStudentId());
        dto.setStatus(sub.getStatus());
        dto.setFinalScore(sub.getFinalScore());
        dto.setTeacherComment(sub.getTeacherComment());
        dto.setSubmittedAt(sub.getSubmittedAt());
        dto.setGradedAt(sub.getGradedAt());

        User student = userMapper.selectById(sub.getStudentId());
        if (student != null) {
            dto.setStudentName(student.getUsername());
            dto.setStudentEmail(student.getEmail());
        } else {
            dto.setStudentName("Unknown");
        }

        dto.setAttachmentUrls(toSignedUrls(sub.getAttachmentUrls()));
        return dto;
    }
}
