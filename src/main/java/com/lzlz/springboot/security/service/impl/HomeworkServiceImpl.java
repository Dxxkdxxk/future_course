package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.dto.QuestionDisplayDto;
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
import com.lzlz.springboot.security.dto.StudentHomeworkDetailDto;
import com.lzlz.springboot.security.service.MinIOService;           // [新增]
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

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
    private MinIOService minIOService; // 用于生成文件链接
    /**
     * [核心智能校验方法]
     * 根据传入的 ID 进行层级匹配校验。参数如果为 null 则跳过对应层级的校验。
     *
     *
     @param
     courseId     课程ID (可选)
     *
     @param
     homeworkId   作业ID (可选)
     *
     @param
     submissionId 提交ID (可选)
     */
    private void validateRelation(Long courseId, Long homeworkId, Long submissionId)
    {
        // 1. 检查提交记录 (如果提供了 submissionId)
        if (submissionId != null) {
            HomeworkSubmission sub = submissionMapper.selectById(submissionId);
            if (sub == null) {
                throw new ResourceNotFoundException("找不到提交记录: " + submissionId);
            }
            // 校验: 提交是否属于该作业
            if (homeworkId != null && !sub.getHomeworkId().equals(homeworkId)) {
                throw new CustomGraphException(400, "参数冲突: 提交记录(" + submissionId + ") 不属于作业(" + homeworkId + ")");
            }
            // 关键点: 即使入参 homeworkId 是 null，我们现在也知道了真正的 homeworkId，可以用于后续查 course
            homeworkId = sub.getHomeworkId();
        }

        // 2. 检查作业记录 (如果提供了 homeworkId，或者从上面步骤获取到了)
        if (homeworkId != null) {
            Homework hw = homeworkMapper.selectById(homeworkId);
            if (hw == null) {
                throw new ResourceNotFoundException("找不到作业: " + homeworkId);
            }
            // 校验: 作业是否属于该课程
            if (courseId != null && !hw.getCourseId().equals(courseId)) {
                throw new CustomGraphException(400, "参数冲突: 作业(" + homeworkId + ") 不属于课程(" + courseId + ")");
            }
        }
    }

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
    public List<StudentHomeworkDetailDto> getHomeworkListForStudent(Long courseId, Long studentId) {
        // 1. 先查询该课程下的所有作业
        QueryWrapper<Homework> homeworkWrapper = new QueryWrapper<>();
        homeworkWrapper.eq("course_id", courseId);
        homeworkWrapper.orderByDesc("created_at"); // 按发布时间倒序
        List<Homework> homeworkList = homeworkMapper.selectList(homeworkWrapper);

        List<StudentHomeworkDetailDto> resultList = new ArrayList<>();

        // 2. 遍历每一个作业，检查该学生的提交状态
        for (Homework hw : homeworkList) {
            StudentHomeworkDetailDto dto = new StudentHomeworkDetailDto();

            // 复制作业基本信息
            dto.setId(hw.getId());
            dto.setTitle(hw.getTitle());
            dto.setDeadline(hw.getDeadline());
            // dto.setDescription(hw.getDescription()); // 列表页通常不需要详情，可不传

            // 3. [关键步骤] 查询当前学生对该作业的提交记录
            QueryWrapper<HomeworkSubmission> subWrapper = new QueryWrapper<>();
            subWrapper.eq("homework_id", hw.getId());
            subWrapper.eq("student_id", studentId);
            HomeworkSubmission sub = submissionMapper.selectOne(subWrapper);

            // 4. [修复Status逻辑]
            if (sub != null) {
                // 如果查到了记录，说明已经提交(1) 或 已批改(2)
                dto.setSubmitted(true);
                dto.setStatus(sub.getStatus()); // 使用数据库里的真实状态
            } else {
                // [重点] 如果没查到记录，说明未提交，必须显式设置为 0
                dto.setSubmitted(false);
                dto.setStatus(0); // 0 代表未提交
            }

            resultList.add(dto);
        }

        return resultList;
    }

    @Override
    public HomeworkDetailResponse getHomeworkDetailForTeacher(Long courseId,Long homeworkId) {
        // ... (原有代码保持不变) ...
        validateRelation(courseId, homeworkId,null);
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


    @Override
    public StudentHomeworkDetailDto getHomeworkDetailForStudent(Long courseId, Long homeworkId, Long studentId) {
        // 1. 校验与查询作业
        validateRelation(courseId, homeworkId, null);
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new ResourceNotFoundException("Homework not found: " + homeworkId);
        }

        StudentHomeworkDetailDto dto = new StudentHomeworkDetailDto();
        BeanUtils.copyProperties(homework, dto);

        // ==========================================
        // 2. 查询题目列表
        // ==========================================
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
            questionMap = Collections.emptyMap(); // 记得 import java.util.Collections;
        }

        List<StudentHomeworkDetailDto.StudentQuestionItem> questionItems = new ArrayList<>();
        for (HomeworkQuestion hq : hqList) {
            Question q = questionMap.get(hq.getQuestionId());
            if (q != null) {
                StudentHomeworkDetailDto.StudentQuestionItem item = new StudentHomeworkDetailDto.StudentQuestionItem();

                item.setQuestionId(q.getId()); // 假设 Question ID 是 String
                item.setStem(q.getStem());     // 选项在 Stem 里，前端直接显示 Stem 即可
                item.setType(q.getType());

                // [删除] item.setOptions(...);  <-- 这里去掉了

                item.setScore(hq.getScore());
                item.setSortOrder(hq.getSortOrder());

                questionItems.add(item);
            }
        }
        dto.setQuestions(questionItems);

        // ==========================================
        // 3. 查询提交状态
        // ==========================================
        QueryWrapper<HomeworkSubmission> subWrapper = new QueryWrapper<>();
        subWrapper.eq("homework_id", homeworkId);
        subWrapper.eq("student_id", studentId);
        HomeworkSubmission sub = submissionMapper.selectOne(subWrapper);

        if (sub != null) {
            dto.setSubmitted(true);
            dto.setStatus(sub.getStatus());

            // [删除] dto.setContent(sub.getContent()); <-- 这里去掉了

            dto.setFinalScore(sub.getFinalScore());
            dto.setTeacherComment(sub.getTeacherComment());

            // 处理附件 (这是学生唯一的作答内容)
            String objectNameStr = sub.getAttachmentUrls();
            if (objectNameStr != null && !objectNameStr.isEmpty()) {
                List<String> urls = new ArrayList<>();
                for (String name : objectNameStr.split(",")) {
                    if (!name.trim().isEmpty()) {
                        try {
                            urls.add(minIOService.getPresignedUrl(name.trim()));
                        } catch (Exception e) {
                            // ignore error
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