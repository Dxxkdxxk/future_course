package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.entity.HomeworkQuestion;
import com.lzlz.springboot.security.entity.Question;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.HomeworkMapper;
import com.lzlz.springboot.security.mapper.HomeworkQuestionMapper;
import com.lzlz.springboot.security.mapper.QuestionMapper;
import com.lzlz.springboot.security.service.HomeworkService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createHomework(Long courseId, CreateHomeworkRequest request) {
        // ... (原有代码保持不变) ...
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
    }

    /**
     * (新增) 实现获取列表方法
     */
    @Override
    public List<Homework> getHomeworkList(Long courseId) {
        QueryWrapper<Homework> wrapper = new QueryWrapper<>();
        // 对应数据库字段 course_id
        wrapper.eq("course_id", courseId);
        // 按创建时间倒序排列 (对应数据库字段 created_at)
        wrapper.orderByDesc("created_at");

        return homeworkMapper.selectList(wrapper);
    }

    /**
     * (新增) 学生端列表查询实现
     * 1. 必须是当前课程 (courseId)
     * 2. 必须是已发布状态 (status = 1)
     * 3. 按发布时间/创建时间倒序
     */
    @Override
    public List<Homework> getHomeworkListForStudent(Long courseId) {
        QueryWrapper<Homework> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.eq("status", 1); // 1: 已发布
        wrapper.orderByDesc("created_at");

        return homeworkMapper.selectList(wrapper);
    }

    @Override
    public HomeworkDetailResponse getHomeworkDetailForTeacher(Long homeworkId) {
        // ... (原有代码保持不变) ...
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
            questionMap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
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

        return response;
    }
}