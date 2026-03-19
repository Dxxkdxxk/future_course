package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lzlz.springboot.security.constants.RedisKeys;
import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.dto.StudentHomeworkDetailDto;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.entity.HomeworkQuestion;
import com.lzlz.springboot.security.entity.HomeworkSubmission;
import com.lzlz.springboot.security.entity.Question;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.HomeworkMapper;
import com.lzlz.springboot.security.mapper.HomeworkQuestionMapper;
import com.lzlz.springboot.security.mapper.HomeworkSubmissionMapper;
import com.lzlz.springboot.security.mapper.QuestionMapper;
import com.lzlz.springboot.security.service.HomeworkService;
import com.lzlz.springboot.security.service.MinIOService;
import com.lzlz.springboot.security.service.RedisCacheService;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HomeworkServiceImpl implements HomeworkService {

    @Autowired
    private HomeworkMapper homeworkMapper;

    @Autowired
    private HomeworkQuestionMapper homeworkQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

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
                throw new ResourceNotFoundException("未找到作业提交记录: " + submissionId);
            }
            if (homeworkId != null && !sub.getHomeworkId().equals(homeworkId)) {
                throw new CustomGraphException(400, "参数不一致: 提交记录(" + submissionId + ") 不属于作业(" + homeworkId + ")");
            }
            homeworkId = sub.getHomeworkId();
        }

        if (homeworkId != null) {
            Homework hw = homeworkMapper.selectById(homeworkId);
            if (hw == null) {
                throw new ResourceNotFoundException("未找到作业: " + homeworkId);
            }
            if (courseId != null && !hw.getCourseId().equals(courseId)) {
                throw new CustomGraphException(400, "参数不一致: 作业(" + homeworkId + ") 不属于课程(" + courseId + ")");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createHomework(Long courseId, CreateHomeworkRequest request) {
        Homework homework = new Homework();
        homework.setCourseId(courseId);
        homework.setTitle(request.getTitle());
        homework.setDescription(request.getDescription());
        homework.setDeadline(request.getDeadline());
        homework.setStatus(1);
        homeworkMapper.insert(homework);

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            int sortOrder = 1;
            for (CreateHomeworkRequest.QuestionItem item : request.getQuestions()) {
                HomeworkQuestion relation = new HomeworkQuestion();
                relation.setHomeworkId(homework.getId());
                relation.setQuestionId(item.getQuestionId());
                relation.setScore(item.getScore());
                relation.setSortOrder(sortOrder++);
                homeworkQuestionMapper.insert(relation);
            }
        }

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

        List<StudentHomeworkDetailDto> resultList = new ArrayList<>();

        for (Homework hw : homeworkList) {
            StudentHomeworkDetailDto dto = new StudentHomeworkDetailDto();
            dto.setId(hw.getId());
            dto.setTitle(hw.getTitle());
            dto.setDeadline(hw.getDeadline());

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

        QueryWrapper<HomeworkQuestion> hqWrapper = new QueryWrapper<>();
        hqWrapper.eq("homework_id", homeworkId);
        hqWrapper.orderByAsc("sort_order");
        List<HomeworkQuestion> hqList = homeworkQuestionMapper.selectList(hqWrapper);

        List<String> questionIds = hqList.stream()
                .map(HomeworkQuestion::getQuestionId)
                .collect(Collectors.toList());

        Map<String, Question> questionMap;
        if (!questionIds.isEmpty()) {
            List<Question> questions = questionMapper.selectBatchIds(questionIds);
            questionMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        } else {
            questionMap = Map.of();
        }

        List<HomeworkDetailResponse.QuestionDetailItem> questionItems = new ArrayList<>();
        for (HomeworkQuestion hq : hqList) {
            Question q = questionMap.get(hq.getQuestionId());
            if (q != null) {
                HomeworkDetailResponse.QuestionDetailItem item = new HomeworkDetailResponse.QuestionDetailItem();
                item.setQuestionId(q.getId());
                item.setStem(q.getStem());
                item.setType(q.getType());
                item.setScore(hq.getScore());
                item.setSortOrder(hq.getSortOrder());
                item.setAnswer(q.getAnswer());
                item.setAnalysis(q.getAnalysis());
                questionItems.add(item);
            }
        }

        HomeworkDetailResponse response = new HomeworkDetailResponse();
        BeanUtils.copyProperties(homework, response);
        response.setQuestions(questionItems);
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
        BeanUtils.copyProperties(homework, dto);

        QueryWrapper<HomeworkQuestion> hqWrapper = new QueryWrapper<>();
        hqWrapper.eq("homework_id", homeworkId);
        hqWrapper.orderByAsc("sort_order");
        List<HomeworkQuestion> hqList = homeworkQuestionMapper.selectList(hqWrapper);

        List<String> questionIds = hqList.stream()
                .map(HomeworkQuestion::getQuestionId)
                .collect(Collectors.toList());

        Map<String, Question> questionMap;
        if (!questionIds.isEmpty()) {
            List<Question> questions = questionMapper.selectBatchIds(questionIds);
            questionMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        } else {
            questionMap = Collections.emptyMap();
        }

        List<StudentHomeworkDetailDto.StudentQuestionItem> questionItems = new ArrayList<>();
        for (HomeworkQuestion hq : hqList) {
            Question q = questionMap.get(hq.getQuestionId());
            if (q != null) {
                StudentHomeworkDetailDto.StudentQuestionItem item = new StudentHomeworkDetailDto.StudentQuestionItem();
                item.setQuestionId(q.getId());
                item.setStem(q.getStem());
                item.setType(q.getType());
                item.setScore(hq.getScore());
                item.setSortOrder(hq.getSortOrder());
                questionItems.add(item);
            }
        }
        dto.setQuestions(questionItems);

        QueryWrapper<HomeworkSubmission> subWrapper = new QueryWrapper<>();
        subWrapper.eq("homework_id", homeworkId);
        subWrapper.eq("student_id", studentId);
        HomeworkSubmission sub = submissionMapper.selectOne(subWrapper);

        if (sub != null) {
            dto.setSubmitted(true);
            dto.setStatus(sub.getStatus());
            dto.setFinalScore(sub.getFinalScore());
            dto.setTeacherComment(sub.getTeacherComment());

            String objectNameStr = sub.getAttachmentUrls();
            if (objectNameStr != null && !objectNameStr.isEmpty()) {
                List<String> urls = new ArrayList<>();
                for (String name : objectNameStr.split(",")) {
                    if (!name.trim().isEmpty()) {
                        try {
                            urls.add(minIOService.getPresignedUrl(name.trim()));
                        } catch (Exception ignored) {
                        }
                    }
                }
                dto.setAttachmentUrls(urls);
            }
        } else {
            dto.setSubmitted(false);
            dto.setStatus(0);
        }

        return dto;
    }
}
