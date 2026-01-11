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
    private QuestionMapper questionMapper; // (!!!) 新增注入

    @Override
    @Transactional(rollbackFor = Exception.class) // 保证主表和关联表同时成功或失败
    public void createHomework(Long courseId, CreateHomeworkRequest request) {
        // 1. 保存作业主表信息
        Homework homework = new Homework();
        homework.setCourseId(courseId);
        homework.setTitle(request.getTitle());
        homework.setDescription(request.getDescription());
        homework.setDeadline(request.getDeadline());
        homework.setStatus(1); // 1: 已发布

        homeworkMapper.insert(homework); // Mybatis-Plus 会自动回填 homework.id

        // 2. 保存题目关联信息
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            int sortOrder = 1;
            for (CreateHomeworkRequest.QuestionItem item : request.getQuestions()) {
                HomeworkQuestion relation = new HomeworkQuestion();
                relation.setHomeworkId(homework.getId()); // 使用刚才生成的主键
                relation.setQuestionId(item.getQuestionId());
                relation.setScore(item.getScore());
                relation.setSortOrder(sortOrder++);

                homeworkQuestionMapper.insert(relation);
            }
        }
    }

    @Override
    public HomeworkDetailResponse getHomeworkDetailForTeacher(Long homeworkId) {
        // 1. 查询作业主表
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new ResourceNotFoundException("Homework not found with id: " + homeworkId);
        }

        // 2. 查询关联表 (HomeworkQuestion)，按 sortOrder 排序
        QueryWrapper<HomeworkQuestion> hqWrapper = new QueryWrapper<>();
        hqWrapper.eq("homework_id", homeworkId);
        hqWrapper.orderByAsc("sort_order");
        List<HomeworkQuestion> hqList = homeworkQuestionMapper.selectList(hqWrapper);

        // 3. 提取所有 QuestionID 并批量查询 Question 表 (减少数据库交互次数)
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

        // 4. 组装 QuestionDetailItem 列表
        List<HomeworkDetailResponse.QuestionDetailItem> questionItems = new ArrayList<>();
        for (HomeworkQuestion hq : hqList) {
            Question q = questionMap.get(hq.getQuestionId());
            if (q != null) {
                HomeworkDetailResponse.QuestionDetailItem item = new HomeworkDetailResponse.QuestionDetailItem();
                item.setQuestionId(q.getId());
                item.setStem(q.getStem());
                item.setType(q.getType());
                // 注意：这里使用作业中设定的分数，而不是题目原本的默认分数
                item.setScore(hq.getScore());
                item.setSortOrder(hq.getSortOrder());
                item.setAnswer(q.getAnswer());     // 教师端可以看到答案
                item.setAnalysis(q.getAnalysis()); // 教师端可以看到解析
                questionItems.add(item);
            }
        }

        // 5. 组装最终 Response
        HomeworkDetailResponse response = new HomeworkDetailResponse();
        BeanUtils.copyProperties(homework, response);
        response.setQuestions(questionItems);

        return response;
    }
}

