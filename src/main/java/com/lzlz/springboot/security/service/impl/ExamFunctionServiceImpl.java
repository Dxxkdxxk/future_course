package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.ExamFunctionDto;
import com.lzlz.springboot.security.entity.*;
import com.lzlz.springboot.security.mapper.*;
// [修复 2] 导入 User 类
import com.lzlz.springboot.security.security.User;
import com.lzlz.springboot.security.service.ExamFunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExamFunctionServiceImpl implements ExamFunctionService {

    @Autowired private
    PaperMapper paperMapper;
    @Autowired private
    PaperQuestionMapper paperQuestionMapper;
    @Autowired private
    QuestionMapper questionMapper;
    @Autowired private
    TestTaskMapper testTaskMapper;
    @Autowired private
    CourseClassMapper courseClassMapper;
    @Autowired private
    ClassStudentMapper classStudentMapper;
    @Autowired private
    StudentPaperRecordMapper studentPaperRecordMapper;
    @Autowired private
    StudentPaperDetailMapper studentPaperDetailMapper;

    // [修复 3] 注入 UserMapper
    @Autowired private
    UserMapper userMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishExam(ExamFunctionDto.PublishRequest request, Integer teacherId) {
        // --- 0. [修复点] 根据 classId 查找 courseId ---
        Long courseId = getCourseIdByClass(request.getClassId());

        // --- 1. [隐式组卷] 创建 Paper 对象 ---
        Paper paper = new Paper();
        paper.setTitle(request.getTitle());
        paper.setDuration(request.getDuration());
        paper.setPassScore(request.getPassScore());
        paper.setStatus(1);

        // 设置课程ID
        paper.setCourseId(courseId);

        paperMapper.insert(paper);

        // --- 2. [关联题目] (保持不变) ---
        int totalScore = 0;
        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            List<Question> questions = questionMapper.selectBatchIds(request.getQuestionIds());

            for (int i = 0; i < request.getQuestionIds().size(); i++) {
                String qId = request.getQuestionIds().get(i);
                Question q = questions.stream()
                        .filter(item -> item.getId().equals(qId))
                        .findFirst()
                        .orElse(null);

                if (q != null) {
                    PaperQuestion pq = new PaperQuestion();
                    pq.setPaperId(paper.getId());
                    pq.setQuestionId(qId);
                    pq.setSortOrder(i + 1);
                    pq.setScore(q.getScore());
                    paperQuestionMapper.insert(pq);
                    totalScore += (q.getScore() == null ? 0 : q.getScore());
                }
            }
        }
        paper.setTotalScore(totalScore);
        paperMapper.updateById(paper);

        // --- 3. [发布任务] (保持不变) ---
        TestTask task = new TestTask();
        task.setPaperId(paper.getId());
        task.setClassId(request.getClassId());
        task.setTitle(request.getTitle());
        task.setStartTime(request.getStartTime());
        task.setDeadline(request.getDeadline());
        task.setDuration(request.getDuration());
        task.setStatus(0);

        testTaskMapper.insert(task);

        return task.getId();
    }


    @Override
    public
    List<ExamFunctionDto.TaskSummary> getTeacherTaskList(Long classId) {
        // 1. 查出该班级所有的测试任务
        QueryWrapper<TestTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("class_id", classId);
        taskWrapper.orderByDesc("created_at"); // 按创建时间倒序
        List<TestTask> tasks = testTaskMapper.selectList(taskWrapper);

        List<ExamFunctionDto.TaskSummary> resultList = new ArrayList<>();

        if (tasks == null || tasks.isEmpty()) {
            return resultList;
        }

        // 2. 查班级总人数 (所有任务的班级总人数是一样的)
        QueryWrapper<ClassStudent> classStudentWrapper = new QueryWrapper<>();
        classStudentWrapper.eq("class_id", classId);
        Long totalStudents = classStudentMapper.selectCount(classStudentWrapper);

        // 3. 遍历封装数据
        for (TestTask task : tasks) {
            ExamFunctionDto.TaskSummary summary = new ExamFunctionDto.TaskSummary();
            summary.setTaskId(task.getId());
            summary.setPaperId(task.getPaperId());
            summary.setTitle(task.getTitle());
            summary.setStartTime(task.getStartTime());
            summary.setDeadline(task.getDeadline());

            // 动态计算状态 (辅助前端展示)
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(task.getStartTime())) {
                summary.setStatus(0); // 未开始
            }
            else if
            (now.isAfter(task.getDeadline())) {summary.setStatus(2); // 已结束
            }
            else {
                summary.setStatus(1); // 进行中
            }
            // 也可以直接使用数据库存储的 status: summary.setStatus(task.getStatus());

            summary.setTotalStudentCount(totalStudents.intValue());

            // 4. 统计已提交人数
            // 逻辑：因为我们是“隐式组卷”，每次发布的 Paper 都是新的且唯一的
            // 所以直接统计该 paper_id 下有多少条 StudentPaperRecord 即可
            QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("paper_id", task.getPaperId());
            // 注意：有些逻辑可能是“只要点击开始考试就生成记录”，
            // 如果要统计“真正交卷”的人数，可能需要加 .ge("status", 1) (假设1是已提交)
            // 这里暂且统计“参与人数”
            Long submittedCount = studentPaperRecordMapper.selectCount(recordWrapper);
            summary.setSubmittedCount(submittedCount.intValue());

            resultList.add(summary);
        }

        return resultList;
    }


    @Override
    public
    List<ExamFunctionDto.StudentTaskView> getStudentTasks(Long classId, Integer studentId) {
        // 1. 初始化结果列表 (!!! 关键点：一开始就创建空列表，绝对不返回 null)
        List<ExamFunctionDto.StudentTaskView> resultList = new ArrayList<>();
        // 1. 查出该班级发布的所有测试任务
        QueryWrapper<TestTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("class_id", classId);
        taskWrapper.orderByDesc("created_at"); // 最新发布的在前
        List<TestTask> tasks = testTaskMapper.selectList(taskWrapper);

        // 3. 如果没有任务，直接返回这个空列表 []
        // 不要返回 null !
        if (tasks == null || tasks.isEmpty()) {
            return resultList;
        }
        LocalDateTime now = LocalDateTime.now();


        // 2. 遍历任务，查询学生的考试记录
        for (TestTask task : tasks) {
            ExamFunctionDto.StudentTaskView view = new ExamFunctionDto.StudentTaskView();
            view.setTaskId(task.getId());
            view.setPaperId(task.getPaperId());
            view.setTitle(task.getTitle());
            view.setStartTime(task.getStartTime());
            view.setDeadline(task.getDeadline());
            view.setDuration(task.getDuration());

            // --- A. 计算任务的时间状态 (Task Status) ---
            if (now.isBefore(task.getStartTime())) {
                view.setTaskStatus(0); // 未开始
            }
            else if (now.isAfter(task.getDeadline())) {
                view.setTaskStatus(2); // 已结束
            }
            else {
                view.setTaskStatus(1); // 进行中
            }

            // --- B. 查询学生的个人状态 (My Status) ---
            // 根据 paperId 和 studentId 查询记录
            // 注意：这里假设一个 paperId 对应一次任务（隐式组卷逻辑下是成立的）
            QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("paper_id", task.getPaperId());
            recordWrapper.eq("student_id", studentId);
            recordWrapper.last("LIMIT 1");

            StudentPaperRecord record = studentPaperRecordMapper.selectOne(recordWrapper);

            if (record == null) {
                // 没有记录 -> 未参加
                view.setMyStatus(0);
                view.setMyScore(null);
            }
            else {
                // 有记录 -> 读取记录状态
                // 假设 StudentPaperRecord 表里有 status 字段: 1=已提交(待批改), 2=已完成
                // 如果您还没加 status 字段，可以根据 totalScore 是否为空来判断，但建议加字段更严谨

                // 这里使用 record.getStatus()，如果您的实体类还没加这个字段，请务必加上
                view.setMyStatus(record.getStatus() != null ? record.getStatus() : 1);

                if (view.getMyStatus() == 2) {
                    view.setMyScore(record.getTotalScore());
                }
                else {
                    view.setMyScore(null); // 待批改时不显示分数
                }
            }

            resultList.add(view);
        }

        return resultList;
    }


    // 在 ExamFunctionServiceImpl.java 中添加以下方法

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamFunctionDto.PaperView getPaperContentByTask(Long taskId, Integer studentId) {
        // 1. 获取测试任务详情
        TestTask task = testTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("测试任务不存在"); // 建议用自定义异常 ResourceNotFoundException
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. 时间校验
        if (now.isBefore(task.getStartTime())) {
            throw new RuntimeException("考试尚未开始，请耐心等待");
        }
        if (now.isAfter(task.getDeadline())) {
            throw new RuntimeException("考试已截止，无法进入"); // 或者允许进入查看但不能提交
        }

        // 3. [关键] 检查或创建考试记录
        QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("paper_id", task.getPaperId());
        recordWrapper.eq("student_id", studentId);
        StudentPaperRecord record = studentPaperRecordMapper.selectOne(recordWrapper);

        if (record != null) {
            // 如果已有记录，检查状态
            if (record.getStatus() != null && record.getStatus() >= 1) {
                throw new RuntimeException("您已提交过试卷，不能重复考试");
            }
            // 如果 status == 0，说明是中途退出又回来的，允许继续
        } else {
            // 如果没有记录，说明是第一次进入 -> 创建"答题中"记录
            record = new StudentPaperRecord();
            record.setPaperId(task.getPaperId());
            record.setStudentId(studentId);
            record.setChapterId(0L); // 班级测试不挂章节
            record.setStatus(0);     // 0: 答题中
            record.setTotalScore(0);
            record.setIsPassed(false);
            studentPaperRecordMapper.insert(record);
        }

        // 4. 获取试卷基础信息
        Paper paper = paperMapper.selectById(task.getPaperId());
        if (paper == null) {
            throw new RuntimeException("试卷数据丢失");
        }

        // 5. 获取题目列表 (PaperQuestion -> Question)
        // 5.1 先查关联表，按 sort_order 排序
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", paper.getId());
        pqWrapper.orderByAsc("sort_order");
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        if (paperQuestions.isEmpty()) {
            throw new RuntimeException("该试卷暂无题目，请联系老师");
        }

        // 5.2 提取题目ID列表
        List<String> questionIds = paperQuestions.stream()
                .map(PaperQuestion::getQuestionId)
                .toList();

        // 5.3 批量查题目详情
        List<Question> questions = questionMapper.selectBatchIds(questionIds);

        // 6. 组装返回视图 (DTO转换 + 脱敏)
        ExamFunctionDto.PaperView view = new ExamFunctionDto.PaperView();
        view.setTaskId(taskId);
        view.setPaperId(paper.getId());
        view.setTitle(task.getTitle()); // 优先展示任务标题
        view.setDuration(task.getDuration());
        view.setDeadline(task.getDeadline());

        // 计算剩余秒数 (截止时间 - 当前时间)
        long secondsLeft = java.time.Duration.between(now, task.getDeadline()).getSeconds();
        // 如果有限时(duration)，还需要比较 (开始时间+duration) 与 截止时间 哪个更早
        if (task.getDuration() != null && task.getDuration() > 0) {
            // 逻辑比较复杂，简单起见先返回截止倒计时
        }
        view.setRemainingSeconds((int) Math.max(0, secondsLeft));

        List<ExamFunctionDto.QuestionItem> questionItems = new ArrayList<>();

        // 双重循环保证顺序 (因为 selectBatchIds 不保证顺序)
        for (PaperQuestion pq : paperQuestions) {
            Question q = questions.stream()
                    .filter(item -> item.getId().equals(pq.getQuestionId()))
                    .findFirst()
                    .orElse(null);

            if (q != null) {
                ExamFunctionDto.QuestionItem item = new ExamFunctionDto.QuestionItem();
                item.setId(q.getId());
                item.setType(q.getType());
                item.setStem(q.getStem());
                item.setScore(pq.getScore()); // 使用试卷中设定的分数
                item.setSortOrder(pq.getSortOrder());

                // ⚠️ 重要：绝对不要设置 item.setAnswer(q.getAnswer())

                questionItems.add(item);
            }
        }
        view.setQuestions(questionItems);

        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamFunctionDto.SubmitResult submitPaper(Long taskId, Integer studentId, ExamFunctionDto.SubmitRequest request) {
        // 1. 校验任务与时间
        TestTask task = testTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");

        LocalDateTime now = LocalDateTime.now();
        // 允许有一点延迟提交的宽容度(例如5分钟)，或者严格限制
        if (now.isAfter(task.getDeadline().plusMinutes(5))) {
            throw new RuntimeException("已超过截止时间，无法提交");
        }

        // 2. 获取考试记录
        QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("paper_id", task.getPaperId());
        recordWrapper.eq("student_id", studentId);
        StudentPaperRecord record = studentPaperRecordMapper.selectOne(recordWrapper);

        if (record == null) {
            // 极其罕见的情况：学生没点开始考试直接调提交接口
            throw new RuntimeException("未找到考试记录，请先开始考试");
        }
        if (record.getStatus() >= 1) {
            throw new RuntimeException("您已交卷，请勿重复提交");
        }

        // 3. 获取试卷所有题目信息 (含标准答案)
        // 3.1 查 PaperQuestion 获取分值配置
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", task.getPaperId());
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        // 3.2 查 Question 获取标准答案和题型
        List<String> qIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<String, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 4. 判分逻辑
        int totalScore = 0;
        boolean hasSubjective = false; // 是否包含主观题
        Map<String, String> studentAnswers = request.getAnswers() != null ? request.getAnswers() : new HashMap<>();

        List<StudentPaperDetail> details = new ArrayList<>();

        for (PaperQuestion pq : paperQuestions) {
            String qId = pq.getQuestionId();
            Question q = questionMap.get(qId);
            if (q == null) continue;

            String studentAns = studentAnswers.getOrDefault(qId, ""); // 学生答案
            String standardAns = q.getAnswer(); // 标准答案

            StudentPaperDetail detail = new StudentPaperDetail();
            detail.setRecordId(record.getId());
            detail.setQuestionId(qId);
            detail.setStudentAnswer(studentAns);

            // --- 自动判分核心 ---
            boolean isCorrect = false;
            int gainedScore = 0;

            if ("简答题".equals(q.getType()) || "代码题".equals(q.getType()) || "主观题".equals(q.getType())) {
                // 主观题：不自动判分，交给老师
                hasSubjective = true;
                detail.setIsCorrect(false); // 暂时标记为错或null
                detail.setScoreGained(0);   // 暂时0分
            } else {
                // 客观题 (单选/多选/判断/填空)
                if ("填空题".equals(q.getType())) {
                    // 填空题特殊处理：分号分割
                    isCorrect = checkFillInBlank(studentAns, standardAns);
                } else {
                    // 选择/判断：直接比对字符串 (忽略大小写和首尾空格可能更友好)
                    isCorrect = studentAns.trim().equalsIgnoreCase(standardAns != null ? standardAns.trim() : "");
                }

                if (isCorrect) {
                    gainedScore = pq.getScore(); // 答对得满分
                }
                detail.setIsCorrect(isCorrect);
                detail.setScoreGained(gainedScore);
            }

            totalScore += gainedScore;
            details.add(detail);
        }

        // 5. 保存答题详情 (批量插入)
        for (StudentPaperDetail detail : details) {
            studentPaperDetailMapper.insert(detail);
        }

        // 6. 更新总记录状态
        record.setTotalScore(totalScore);
        if (hasSubjective) {
            record.setStatus(1); // 1: 待批改
        } else {
            record.setStatus(2); // 2: 已完成
        }
        // 简单的及格判定 (假设60%及格，您也可以在Paper表加passScore字段)
        // record.setIsPassed(totalScore >= record.getFullScore() * 0.6);

        studentPaperRecordMapper.updateById(record);

        // 7. 返回结果
        ExamFunctionDto.SubmitResult result = new ExamFunctionDto.SubmitResult();
        if (hasSubjective) {
            result.setFinalScore(null);
            result.setMessage("交卷成功！主观题需等待教师批改，客观题得分：" + totalScore);
        } else {
            result.setFinalScore(totalScore);
            result.setMessage("交卷成功！您的成绩是：" + totalScore);
        }
        return result;
    }



    @Override
    public List<ExamFunctionDto.StudentSubmissionDto> getTaskSubmissions(Long taskId) {
        // 1. 获取任务信息
        TestTask task = testTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");

        // 2. 获取该班级所有学生ID
        QueryWrapper<ClassStudent> classStuWrapper = new QueryWrapper<>();
        classStuWrapper.eq("class_id", task.getClassId());
        List<ClassStudent> classStudents = classStudentMapper.selectList(classStuWrapper);

        if (classStudents.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> studentIds = classStudents.stream()
                .map(ClassStudent::getUserId)
                .toList();

        // 3. 批量查询学生基本信息 (姓名、学号)
        List<User> users = userMapper.selectBatchIds(studentIds);
        Map<Integer, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 4. 获取该试卷的所有考试记录
        QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("paper_id", task.getPaperId());
        List<StudentPaperRecord> records = studentPaperRecordMapper.selectList(recordWrapper);

        // 转为 Map: Key=studentId, Value=Record
        Map<Integer, StudentPaperRecord> recordMap = records.stream()
                .collect(Collectors.toMap(StudentPaperRecord::getStudentId, r -> r));

        // 5. 组装结果列表 (以班级名单为准)
        List<ExamFunctionDto.StudentSubmissionDto> resultList = new ArrayList<>();

        for (Integer sId : studentIds) {
            User student = userMap.get(sId);
            if (student == null) continue; // 防御性编程

            StudentPaperRecord record = recordMap.get(sId);
            ExamFunctionDto.StudentSubmissionDto dto = new ExamFunctionDto.StudentSubmissionDto();

            dto.setStudentId(sId);
            // 假设 User 实体中有 getUsername (学号) 和 getNickname (姓名)，如果没有姓名则用用户名
            // 根据您提供的 User.java，只有 username，没有 nickname，这里暂用 username
            dto.setStudentName(student.getUsername());
            dto.setStudentNo(student.getUsername());

            if (record == null) {
                // 未参加
                dto.setRecordId(null);
                dto.setStatus(-1); // -1 代表未参加
                dto.setTotalScore(null);
                dto.setSubmitTime(null);
            } else {
                dto.setRecordId(record.getId());
                // 直接使用记录的状态 (0:答题中, 1:待批改, 2:已完成)
                // 如果您数据库里还没刷数据，可能为null，给个默认值
                int status = record.getStatus() != null ? record.getStatus() : 0;
                dto.setStatus(status);

                // 只有已完成(2)才显示分数，或者待批改(1)也可以显示客观题预估分，看需求
                dto.setTotalScore(record.getTotalScore());

                // 提交时间 (这里暂用创建时间，如果表里有 updated_at 更好)
                dto.setSubmitTime(record.getCreatedAt());
            }

            resultList.add(dto);
        }

        return resultList;
    }


    @Override
    public ExamFunctionDto.GradingView getSubmissionForGrading(Long recordId) {
        // 1. 获取考试记录
        StudentPaperRecord record = studentPaperRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("考试记录不存在");
        }

        // 2. 获取学生信息
        User student = userMapper.selectById(record.getStudentId());
        String studentName = (student != null) ? student.getUsername() : "未知学生";

        // 3. 获取答题详情 (学生答案)
        QueryWrapper<StudentPaperDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("record_id", recordId);
        List<StudentPaperDetail> details = studentPaperDetailMapper.selectList(detailWrapper);
        // 转 Map 方便查找: Key=questionId
        Map<String, StudentPaperDetail> detailMap = details.stream()
                .collect(Collectors.toMap(StudentPaperDetail::getQuestionId, d -> d));

        // 4. 获取试卷题目配置 (分值、顺序)
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", record.getPaperId());
        pqWrapper.orderByAsc("sort_order");
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        // 5. 获取题目原题 (标准答案、题干)
        if (paperQuestions.isEmpty()) {
            throw new RuntimeException("试卷题目数据丢失");
        }
        List<String> qIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<String, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 6. 组装视图
        ExamFunctionDto.GradingView view = new ExamFunctionDto.GradingView();
        view.setRecordId(recordId);
        view.setStudentId(record.getStudentId());
        view.setStudentName(studentName);
        view.setTotalScore(record.getTotalScore());

        List<ExamFunctionDto.GradingQuestionItem> itemList = new ArrayList<>();

        for (PaperQuestion pq : paperQuestions) {
            String qId = pq.getQuestionId();
            Question q = questionMap.get(qId);
            StudentPaperDetail detail = detailMap.get(qId); // 可能为null（如果学生没答这道题）

            if (q != null) {
                ExamFunctionDto.GradingQuestionItem item = new ExamFunctionDto.GradingQuestionItem();
                item.setId(qId);
                item.setStem(q.getStem());
                item.setType(q.getType());
                item.setScore(pq.getScore()); // 本题满分
                item.setSortOrder(pq.getSortOrder());
                item.setStandardAnswer(q.getAnswer()); // 教师端可见标准答案

                // 判断是否为主观题 (用于前端展示“待批改”标记)
                boolean isSubjective = "简答题".equals(q.getType()) || "代码题".equals(q.getType()) || "主观题".equals(q.getType());
                item.setIsSubjective(isSubjective);

                if (detail != null) {
                    item.setStudentAnswer(detail.getStudentAnswer());
                    item.setGainedScore(detail.getScoreGained());
                    item.setIsCorrect(detail.getIsCorrect());
                } else {
                    // 学生未作答
                    item.setStudentAnswer("");
                    item.setGainedScore(0);
                    item.setIsCorrect(false);
                }

                itemList.add(item);
            }
        }
        view.setQuestions(itemList);

        return view;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeSubjectiveQuestions(Long recordId, ExamFunctionDto.GradeRequest request) {
        // 1. 获取考试记录
        StudentPaperRecord record = studentPaperRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("考试记录不存在");
        }

        if (request.getGrades() == null || request.getGrades().isEmpty()) {
            throw new RuntimeException("评分数据不能为空");
        }

        // 2. 预加载试卷题目配置 (为了获取每道题的满分，用于判断 is_correct)
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", record.getPaperId());
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);
        Map<String, Integer> fullScoreMap = paperQuestions.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore));

        // 3. 遍历并更新每一道题的得分
        for (ExamFunctionDto.GradeRequest.QuestionGrade grade : request.getGrades()) {
            // 查出该题的答题详情
            QueryWrapper<StudentPaperDetail> detailWrapper = new QueryWrapper<>();
            detailWrapper.eq("record_id", recordId);
            detailWrapper.eq("question_id", grade.getQuestionId());
            StudentPaperDetail detail = studentPaperDetailMapper.selectOne(detailWrapper);

            if (detail != null) {
                // 更新得分
                detail.setScoreGained(grade.getScore());

                // 更新是否正确 (逻辑：得分 == 满分 则为 true，否则 false)
                Integer fullScore = fullScoreMap.getOrDefault(grade.getQuestionId(), 0);
                boolean isCorrect = fullScore > 0 && grade.getScore().equals(fullScore);
                detail.setIsCorrect(isCorrect);

                studentPaperDetailMapper.updateById(detail);
            }
        }

        // 4. [核心] 重新计算试卷总分
        // 既然刚刚更新了部分题目，现在重新汇总该记录下的所有 details 得分最稳妥
        QueryWrapper<StudentPaperDetail> allDetailsWrapper = new QueryWrapper<>();
        allDetailsWrapper.eq("record_id", recordId);
        List<StudentPaperDetail> allDetails = studentPaperDetailMapper.selectList(allDetailsWrapper);

        int newTotalScore = allDetails.stream()
                .mapToInt(d -> d.getScoreGained() == null ? 0 : d.getScoreGained())
                .sum();

        // 5. 更新主记录
        record.setTotalScore(newTotalScore);
        record.setStatus(2); // 标记为 2: 已完成 (成绩已出)

        // 简单的及格判断 (假设 60% 及格)
        // int passLine = (int) (record.getFullScore() * 0.6);
        // record.setIsPassed(newTotalScore >= passLine);

        studentPaperRecordMapper.updateById(record);
    }


    @Override
    public ExamFunctionDto.StudentResultView getStudentExamResult(Long taskId, Integer studentId) {
        // 1. 获取任务与记录
        TestTask task = testTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");

        QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("paper_id", task.getPaperId());
        recordWrapper.eq("student_id", studentId);
        StudentPaperRecord record = studentPaperRecordMapper.selectOne(recordWrapper);

        // 2. 状态校验 (关键安全逻辑)
        if (record == null) {
            throw new RuntimeException("您未参加该考试");
        }
        // 如果状态不是 2 (已完成)，不展示结果
        // 也可以放宽：如果当前时间超过截止时间 task.getDeadline()，也允许查看(视业务而定)
        if (record.getStatus() == null || record.getStatus() != 2) {
            throw new RuntimeException("试卷正在批改中或未提交，暂无法查看结果");
        }

        // 3. 获取试卷题目配置 (顺序、分值)
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", task.getPaperId());
        pqWrapper.orderByAsc("sort_order");
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        // 计算卷面满分
        int fullScore = paperQuestions.stream().mapToInt(PaperQuestion::getScore).sum();

        // 4. 获取题目详情 (标准答案、解析)
        if (paperQuestions.isEmpty()) return null;
        List<String> qIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<String, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 5. 获取学生作答详情
        QueryWrapper<StudentPaperDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("record_id", record.getId());
        List<StudentPaperDetail> details = studentPaperDetailMapper.selectList(detailWrapper);
        Map<String, StudentPaperDetail> detailMap = details.stream()
                .collect(Collectors.toMap(StudentPaperDetail::getQuestionId, d -> d));

        // 6. 组装视图
        ExamFunctionDto.StudentResultView view = new ExamFunctionDto.StudentResultView();
        view.setTaskId(taskId);
        view.setTitle(task.getTitle());
        view.setTotalScore(record.getTotalScore());
        view.setFullScore(fullScore);

        List<ExamFunctionDto.ResultQuestionItem> itemList = new ArrayList<>();

        for (PaperQuestion pq : paperQuestions) {
            String qId = pq.getQuestionId();
            Question q = questionMap.get(qId);
            StudentPaperDetail detail = detailMap.get(qId);

            if (q != null) {
                ExamFunctionDto.ResultQuestionItem item = new ExamFunctionDto.ResultQuestionItem();
                item.setId(qId);
                item.setStem(q.getStem());
                item.setType(q.getType());
                item.setScore(pq.getScore());
                item.setSortOrder(pq.getSortOrder());

                // 结果页：必须展示标准答案和解析
                item.setStandardAnswer(q.getAnswer());
                item.setAnalysis(q.getAnalysis());

                if (detail != null) {
                    item.setStudentAnswer(detail.getStudentAnswer());
                    item.setGainedScore(detail.getScoreGained());
                    item.setIsCorrect(detail.getIsCorrect());
                } else {
                    item.setStudentAnswer("(未作答)");
                    item.setGainedScore(0);
                    item.setIsCorrect(false);
                }

                itemList.add(item);
            }
        }
        view.setQuestions(itemList);
        return view;
    }

    /**
     * [辅助方法] 填空题判分逻辑
     * 策略：全匹配 (所有空都填对才给分)
     */
    private boolean checkFillInBlank(String studentAns, String standardAns) {
        if (studentAns == null || standardAns == null) return false;

        // 约定：多个空用中文或英文分号隔开
        String[] stdArr = standardAns.split("[;；]");
        String[] stuArr = studentAns.split("[;；]");

        if (stdArr.length != stuArr.length) return false;

        for (int i = 0; i < stdArr.length; i++) {
            if (!stdArr[i].trim().equalsIgnoreCase(stuArr[i].trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * [修复后的辅助方法] 根据班级ID查找其所属的课程ID
     */
    private Long getCourseIdByClass(Long classId) {
        // 修正：CourseClass 实体的主键是 id，直接用 selectById 查询即可
        // 之前的 QueryWrapper 误用了 "class_id" 列名
        CourseClass cc = courseClassMapper.selectById(classId);

        if (cc == null) {
            // 容错处理：如果查不到班级，默认课程ID为0或抛出异常
            return 0L;
        }
        return cc.getCourseId();
    }
}