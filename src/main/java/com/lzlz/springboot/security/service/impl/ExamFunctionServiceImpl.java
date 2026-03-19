package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.ExamFunctionDto;
import com.lzlz.springboot.security.entity.ClassStudent;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.entity.CourseClass;
import com.lzlz.springboot.security.entity.Paper;
import com.lzlz.springboot.security.entity.PaperQuestion;
import com.lzlz.springboot.security.entity.Question;
import com.lzlz.springboot.security.entity.StudentPaperDetail;
import com.lzlz.springboot.security.entity.StudentPaperRecord;
import com.lzlz.springboot.security.entity.TestTask;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.ClassStudentMapper;
import com.lzlz.springboot.security.mapper.CourseClassMapper;
import com.lzlz.springboot.security.mapper.CourseMapper;
import com.lzlz.springboot.security.mapper.PaperMapper;
import com.lzlz.springboot.security.mapper.PaperQuestionMapper;
import com.lzlz.springboot.security.mapper.QuestionMapper;
import com.lzlz.springboot.security.mapper.StudentPaperDetailMapper;
import com.lzlz.springboot.security.mapper.StudentPaperRecordMapper;
import com.lzlz.springboot.security.mapper.TestTaskMapper;
import com.lzlz.springboot.security.mapper.UserMapper;
import com.lzlz.springboot.security.service.ExamFunctionService;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExamFunctionServiceImpl implements ExamFunctionService {

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final TestTaskMapper testTaskMapper;
    private final CourseClassMapper courseClassMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentPaperRecordMapper studentPaperRecordMapper;
    private final StudentPaperDetailMapper studentPaperDetailMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final StudentCourseAccessService studentCourseAccessService;

    public ExamFunctionServiceImpl(PaperMapper paperMapper,
                                   PaperQuestionMapper paperQuestionMapper,
                                   QuestionMapper questionMapper,
                                   TestTaskMapper testTaskMapper,
                                   CourseClassMapper courseClassMapper,
                                   ClassStudentMapper classStudentMapper,
                                   StudentPaperRecordMapper studentPaperRecordMapper,
                                   StudentPaperDetailMapper studentPaperDetailMapper,
                                   UserMapper userMapper,
                                   CourseMapper courseMapper,
                                   StudentCourseAccessService studentCourseAccessService) {
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionMapper = questionMapper;
        this.testTaskMapper = testTaskMapper;
        this.courseClassMapper = courseClassMapper;
        this.classStudentMapper = classStudentMapper;
        this.studentPaperRecordMapper = studentPaperRecordMapper;
        this.studentPaperDetailMapper = studentPaperDetailMapper;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.studentCourseAccessService = studentCourseAccessService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishExam(ExamFunctionDto.PublishRequest request, Integer teacherId) {
        Course course = courseMapper.selectById(request.getCourseId());
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with id: " + request.getCourseId());
        }
        if (teacherId != null && course.getTeacherId() != null && !course.getTeacherId().equals(Long.valueOf(teacherId))) {
            throw new CustomGraphException(403, "No permission to publish exam for this course");
        }

        Paper paper = new Paper();
        paper.setTitle(request.getTitle());
        paper.setDuration(request.getDuration());
        paper.setPassScore(request.getPassScore());
        paper.setStatus(1);
        paper.setCourseId(request.getCourseId());
        paperMapper.insert(paper);

        int totalScore = 0;
        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            List<Question> questions = questionMapper.selectBatchIds(request.getQuestionIds());
            Map<String, Question> questionMap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

            for (int i = 0; i < request.getQuestionIds().size(); i++) {
                String questionId = request.getQuestionIds().get(i);
                Question question = questionMap.get(questionId);
                if (question == null) {
                    continue;
                }
                int score = question.getScore() == null ? 0 : question.getScore();
                PaperQuestion pq = new PaperQuestion();
                pq.setPaperId(paper.getId());
                pq.setQuestionId(questionId);
                pq.setSortOrder(i + 1);
                pq.setScore(score);
                paperQuestionMapper.insert(pq);
                totalScore += score;
            }
        }

        paper.setTotalScore(totalScore);
        paperMapper.updateById(paper);

        TestTask task = new TestTask();
        task.setPaperId(paper.getId());
        task.setCourseId(request.getCourseId());
        task.setTitle(request.getTitle());
        task.setStartTime(request.getStartTime());
        task.setDeadline(request.getDeadline());
        task.setDuration(request.getDuration());
        task.setStatus(0);
        testTaskMapper.insert(task);

        return task.getId();
    }

    @Override
    public List<ExamFunctionDto.TaskSummary> getTeacherTaskList(Long courseId) {
        QueryWrapper<TestTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("course_id", courseId);
        taskWrapper.orderByDesc("created_at");
        List<TestTask> tasks = testTaskMapper.selectList(taskWrapper);

        List<ExamFunctionDto.TaskSummary> result = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return result;
        }

        Long classId = getClassIdByCourse(courseId);
        int totalStudents = 0;
        if (classId != null) {
            QueryWrapper<ClassStudent> classStudentWrapper = new QueryWrapper<>();
            classStudentWrapper.eq("class_id", classId);
            totalStudents = classStudentMapper.selectCount(classStudentWrapper).intValue();
        }

        LocalDateTime now = LocalDateTime.now();
        for (TestTask task : tasks) {
            ExamFunctionDto.TaskSummary summary = new ExamFunctionDto.TaskSummary();
            summary.setTaskId(task.getId());
            summary.setPaperId(task.getPaperId());
            summary.setTitle(task.getTitle());
            summary.setStartTime(task.getStartTime());
            summary.setDeadline(task.getDeadline());
            summary.setStatus(calcTaskStatus(task, now));
            summary.setTotalStudentCount(totalStudents);

            QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("paper_id", task.getPaperId());
            summary.setSubmittedCount(studentPaperRecordMapper.selectCount(recordWrapper).intValue());
            result.add(summary);
        }
        return result;
    }

    @Override
    public List<ExamFunctionDto.StudentTaskView> getStudentTasks(Long courseId, Integer studentId) {
        studentCourseAccessService.checkCourseAccess(studentId, courseId);

        QueryWrapper<TestTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("course_id", courseId);
        taskWrapper.orderByDesc("created_at");
        List<TestTask> tasks = testTaskMapper.selectList(taskWrapper);

        List<ExamFunctionDto.StudentTaskView> result = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        for (TestTask task : tasks) {
            ExamFunctionDto.StudentTaskView view = new ExamFunctionDto.StudentTaskView();
            view.setTaskId(task.getId());
            view.setPaperId(task.getPaperId());
            view.setTitle(task.getTitle());
            view.setStartTime(task.getStartTime());
            view.setDeadline(task.getDeadline());
            view.setDuration(task.getDuration());
            view.setTaskStatus(calcTaskStatus(task, now));

            QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("paper_id", task.getPaperId());
            recordWrapper.eq("student_id", studentId);
            recordWrapper.last("LIMIT 1");
            StudentPaperRecord record = studentPaperRecordMapper.selectOne(recordWrapper);
            if (record == null) {
                view.setMyStatus(0);
                view.setMyScore(null);
            } else {
                int myStatus = record.getStatus() == null ? 1 : record.getStatus();
                view.setMyStatus(myStatus);
                view.setMyScore(myStatus == 2 ? record.getTotalScore() : null);
            }
            result.add(view);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamFunctionDto.PaperView getPaperContentByTask(Long taskId, Integer studentId) {
        studentCourseAccessService.checkTaskAccess(studentId, taskId);

        TestTask task = requireTask(taskId);
        LocalDateTime now = LocalDateTime.now();
        if (task.getStartTime() != null && now.isBefore(task.getStartTime())) {
            throw new RuntimeException("Exam has not started");
        }
        if (task.getDeadline() != null && now.isAfter(task.getDeadline())) {
            throw new RuntimeException("Exam is closed");
        }

        StudentPaperRecord record = getStudentRecord(task.getPaperId(), studentId);
        if (record != null && record.getStatus() != null && record.getStatus() >= 1) {
            throw new RuntimeException("Paper already submitted");
        }
        if (record == null) {
            record = new StudentPaperRecord();
            record.setPaperId(task.getPaperId());
            record.setStudentId(studentId);
            record.setChapterId(0L);
            record.setStatus(0);
            record.setTotalScore(0);
            record.setIsPassed(false);
            studentPaperRecordMapper.insert(record);
        }

        Paper paper = paperMapper.selectById(task.getPaperId());
        if (paper == null) {
            throw new RuntimeException("Paper not found");
        }

        List<PaperQuestion> paperQuestions = getPaperQuestions(task.getPaperId());
        if (paperQuestions.isEmpty()) {
            throw new RuntimeException("No questions in paper");
        }
        Map<String, Question> questionMap = getQuestionMap(paperQuestions);

        ExamFunctionDto.PaperView view = new ExamFunctionDto.PaperView();
        view.setTaskId(task.getId());
        view.setPaperId(paper.getId());
        view.setTitle(task.getTitle());
        view.setDuration(task.getDuration());
        view.setDeadline(task.getDeadline());
        view.setRemainingSeconds((int) Math.max(0, Duration.between(now, task.getDeadline()).getSeconds()));

        List<ExamFunctionDto.QuestionItem> items = new ArrayList<>();
        for (PaperQuestion pq : paperQuestions) {
            Question q = questionMap.get(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            ExamFunctionDto.QuestionItem item = new ExamFunctionDto.QuestionItem();
            item.setId(q.getId());
            item.setType(q.getType());
            item.setStem(q.getStem());
            item.setScore(pq.getScore());
            item.setSortOrder(pq.getSortOrder());
            items.add(item);
        }
        view.setQuestions(items);
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamFunctionDto.SubmitResult submitPaper(Long taskId, Integer studentId, ExamFunctionDto.SubmitRequest request) {
        studentCourseAccessService.checkTaskAccess(studentId, taskId);

        TestTask task = requireTask(taskId);
        LocalDateTime now = LocalDateTime.now();
        if (task.getDeadline() != null && now.isAfter(task.getDeadline().plusMinutes(5))) {
            throw new RuntimeException("Submit timeout");
        }

        StudentPaperRecord record = getStudentRecord(task.getPaperId(), studentId);
        if (record == null) {
            throw new RuntimeException("Record not found, please open paper first");
        }
        if (record.getStatus() != null && record.getStatus() >= 1) {
            throw new RuntimeException("Paper already submitted");
        }

        List<PaperQuestion> paperQuestions = getPaperQuestions(task.getPaperId());
        Map<String, Question> questionMap = getQuestionMap(paperQuestions);
        Map<String, String> answers = request.getAnswers() == null ? new HashMap<>() : request.getAnswers();

        int totalScore = 0;
        boolean hasSubjective = false;
        for (PaperQuestion pq : paperQuestions) {
            Question question = questionMap.get(pq.getQuestionId());
            if (question == null) {
                continue;
            }
            String studentAnswer = answers.getOrDefault(question.getId(), "");
            Integer gainedScore = 0;
            Boolean isCorrect = false;

            if (isSubjectiveQuestion(question.getType())) {
                hasSubjective = true;
            } else {
                isCorrect = checkObjectiveAnswer(question, studentAnswer);
                gainedScore = isCorrect ? (pq.getScore() == null ? 0 : pq.getScore()) : 0;
                totalScore += gainedScore;
            }

            upsertStudentPaperDetail(record.getId(), question.getId(), studentAnswer, isCorrect, gainedScore);
        }

        record.setTotalScore(totalScore);
        record.setStatus(hasSubjective ? 1 : 2);
        if (!hasSubjective) {
            Paper paper = paperMapper.selectById(task.getPaperId());
            if (paper != null && paper.getPassScore() != null) {
                record.setIsPassed(totalScore >= paper.getPassScore());
            }
        }
        studentPaperRecordMapper.updateById(record);

        ExamFunctionDto.SubmitResult result = new ExamFunctionDto.SubmitResult();
        result.setFinalScore(hasSubjective ? null : totalScore);
        result.setMessage(hasSubjective ? "Submitted, waiting for grading" : "Submitted");
        return result;
    }

    @Override
    public List<ExamFunctionDto.StudentSubmissionDto> getTaskSubmissions(Long taskId) {
        TestTask task = requireTask(taskId);
        Long classId = getClassIdByCourse(task.getCourseId());
        if (classId == null) {
            return new ArrayList<>();
        }

        QueryWrapper<ClassStudent> classStudentWrapper = new QueryWrapper<>();
        classStudentWrapper.eq("class_id", classId);
        List<ClassStudent> classStudents = classStudentMapper.selectList(classStudentWrapper);
        if (classStudents.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> studentIds = classStudents.stream().map(ClassStudent::getUserId).distinct().toList();
        List<User> users = userMapper.selectBatchIds(studentIds);
        Map<Integer, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        QueryWrapper<StudentPaperRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("paper_id", task.getPaperId());
        List<StudentPaperRecord> records = studentPaperRecordMapper.selectList(recordWrapper);
        Map<Integer, StudentPaperRecord> recordMap = records.stream()
                .sorted(Comparator.comparing(StudentPaperRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toMap(StudentPaperRecord::getStudentId, Function.identity(), (a, b) -> a));

        List<ExamFunctionDto.StudentSubmissionDto> result = new ArrayList<>();
        for (Integer studentId : studentIds) {
            User user = userMap.get(studentId);
            StudentPaperRecord record = recordMap.get(studentId);

            ExamFunctionDto.StudentSubmissionDto dto = new ExamFunctionDto.StudentSubmissionDto();
            dto.setStudentId(studentId);
            dto.setStudentNo(user == null ? null : user.getUsername());
            dto.setStudentName(user == null ? null : user.getUsername());
            if (record == null) {
                dto.setStatus(-1);
            } else {
                dto.setRecordId(record.getId());
                dto.setStatus(record.getStatus() == null ? 0 : record.getStatus());
                dto.setTotalScore(record.getTotalScore());
                dto.setSubmitTime(record.getCreatedAt());
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public ExamFunctionDto.GradingView getSubmissionForGrading(Long recordId) {
        StudentPaperRecord record = studentPaperRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("Record not found");
        }

        User student = userMapper.selectById(record.getStudentId());
        List<PaperQuestion> paperQuestions = getPaperQuestions(record.getPaperId());
        Map<String, Question> questionMap = getQuestionMap(paperQuestions);
        Map<String, StudentPaperDetail> detailMap = getDetailMap(recordId);

        ExamFunctionDto.GradingView view = new ExamFunctionDto.GradingView();
        view.setRecordId(recordId);
        view.setStudentId(record.getStudentId());
        view.setStudentName(student == null ? null : student.getUsername());
        view.setTotalScore(record.getTotalScore());
        view.setFullScore(paperQuestions.stream().map(PaperQuestion::getScore).filter(Objects::nonNull).mapToInt(Integer::intValue).sum());

        List<ExamFunctionDto.GradingQuestionItem> items = new ArrayList<>();
        for (PaperQuestion pq : paperQuestions) {
            Question q = questionMap.get(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            StudentPaperDetail detail = detailMap.get(q.getId());
            ExamFunctionDto.GradingQuestionItem item = new ExamFunctionDto.GradingQuestionItem();
            item.setId(q.getId());
            item.setStem(q.getStem());
            item.setType(q.getType());
            item.setScore(pq.getScore());
            item.setSortOrder(pq.getSortOrder());
            item.setStandardAnswer(q.getAnswer());
            item.setIsSubjective(isSubjectiveQuestion(q.getType()));
            if (detail != null) {
                item.setStudentAnswer(detail.getStudentAnswer());
                item.setGainedScore(detail.getScoreGained());
                item.setIsCorrect(detail.getIsCorrect());
            } else {
                item.setStudentAnswer("");
                item.setGainedScore(0);
                item.setIsCorrect(false);
            }
            items.add(item);
        }
        view.setQuestions(items);
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeSubjectiveQuestions(Long recordId, ExamFunctionDto.GradeRequest request) {
        StudentPaperRecord record = studentPaperRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("Record not found");
        }
        if (request.getGrades() == null || request.getGrades().isEmpty()) {
            throw new RuntimeException("Grades cannot be empty");
        }

        List<PaperQuestion> paperQuestions = getPaperQuestions(record.getPaperId());
        Map<String, Integer> fullScoreMap = paperQuestions.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq.getScore() == null ? 0 : pq.getScore(), (a, b) -> a));

        for (ExamFunctionDto.GradeRequest.QuestionGrade grade : request.getGrades()) {
            QueryWrapper<StudentPaperDetail> detailWrapper = new QueryWrapper<>();
            detailWrapper.eq("record_id", recordId);
            detailWrapper.eq("question_id", grade.getQuestionId());
            StudentPaperDetail detail = studentPaperDetailMapper.selectOne(detailWrapper);
            if (detail == null) {
                continue;
            }
            int score = grade.getScore() == null ? 0 : grade.getScore();
            detail.setScoreGained(score);
            detail.setIsCorrect(score >= fullScoreMap.getOrDefault(grade.getQuestionId(), 0));
            studentPaperDetailMapper.updateById(detail);
        }

        QueryWrapper<StudentPaperDetail> allDetailWrapper = new QueryWrapper<>();
        allDetailWrapper.eq("record_id", recordId);
        List<StudentPaperDetail> allDetails = studentPaperDetailMapper.selectList(allDetailWrapper);
        int totalScore = allDetails.stream()
                .map(StudentPaperDetail::getScoreGained)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        record.setTotalScore(totalScore);
        record.setStatus(2);

        Paper paper = paperMapper.selectById(record.getPaperId());
        if (paper != null && paper.getPassScore() != null) {
            record.setIsPassed(totalScore >= paper.getPassScore());
        }
        studentPaperRecordMapper.updateById(record);
    }

    @Override
    public ExamFunctionDto.StudentResultView getStudentExamResult(Long taskId, Integer studentId) {
        studentCourseAccessService.checkTaskAccess(studentId, taskId);

        TestTask task = requireTask(taskId);
        StudentPaperRecord record = getStudentRecord(task.getPaperId(), studentId);
        if (record == null) {
            throw new RuntimeException("No exam record");
        }
        if (record.getStatus() == null || record.getStatus() != 2) {
            throw new RuntimeException("Exam is not graded yet");
        }

        List<PaperQuestion> paperQuestions = getPaperQuestions(task.getPaperId());
        Map<String, Question> questionMap = getQuestionMap(paperQuestions);
        Map<String, StudentPaperDetail> detailMap = getDetailMap(record.getId());

        ExamFunctionDto.StudentResultView view = new ExamFunctionDto.StudentResultView();
        view.setTaskId(taskId);
        view.setTitle(task.getTitle());
        view.setTotalScore(record.getTotalScore());
        view.setFullScore(paperQuestions.stream().map(PaperQuestion::getScore).filter(Objects::nonNull).mapToInt(Integer::intValue).sum());

        List<ExamFunctionDto.ResultQuestionItem> items = new ArrayList<>();
        for (PaperQuestion pq : paperQuestions) {
            Question question = questionMap.get(pq.getQuestionId());
            if (question == null) {
                continue;
            }
            StudentPaperDetail detail = detailMap.get(question.getId());

            ExamFunctionDto.ResultQuestionItem item = new ExamFunctionDto.ResultQuestionItem();
            item.setId(question.getId());
            item.setStem(question.getStem());
            item.setType(question.getType());
            item.setScore(pq.getScore());
            item.setSortOrder(pq.getSortOrder());
            item.setStandardAnswer(question.getAnswer());
            item.setAnalysis(question.getAnalysis());
            if (detail != null) {
                item.setStudentAnswer(detail.getStudentAnswer());
                item.setGainedScore(detail.getScoreGained());
                item.setIsCorrect(detail.getIsCorrect());
            } else {
                item.setStudentAnswer("(not answered)");
                item.setGainedScore(0);
                item.setIsCorrect(false);
            }
            items.add(item);
        }
        view.setQuestions(items);
        return view;
    }

    @Override
    public ExamFunctionDto.MyClassInfo getStudentClassByCourse(Integer studentId, Long courseId) {
        studentCourseAccessService.checkCourseAccess(studentId, courseId);

        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getClassId() == null) {
            return null;
        }
        CourseClass courseClass = courseClassMapper.selectById(course.getClassId());
        if (courseClass == null) {
            return null;
        }

        ExamFunctionDto.MyClassInfo info = new ExamFunctionDto.MyClassInfo();
        info.setClassId(courseClass.getId());
        info.setClassName(courseClass.getName());
        info.setCourseId(course.getId());
        info.setCourseName(course.getName());

        if (course.getTeacherId() != null) {
            User teacher = userMapper.selectById(course.getTeacherId());
            if (teacher != null) {
                info.setTeacherName(teacher.getUsername());
            }
        }
        return info;
    }

    private int calcTaskStatus(TestTask task, LocalDateTime now) {
        if (task.getStartTime() != null && now.isBefore(task.getStartTime())) {
            return 0;
        }
        if (task.getDeadline() != null && now.isAfter(task.getDeadline())) {
            return 2;
        }
        return 1;
    }

    private TestTask requireTask(Long taskId) {
        TestTask task = testTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
        return task;
    }

    private StudentPaperRecord getStudentRecord(Long paperId, Integer studentId) {
        QueryWrapper<StudentPaperRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("paper_id", paperId);
        wrapper.eq("student_id", studentId);
        wrapper.last("LIMIT 1");
        return studentPaperRecordMapper.selectOne(wrapper);
    }

    private List<PaperQuestion> getPaperQuestions(Long paperId) {
        QueryWrapper<PaperQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("paper_id", paperId);
        wrapper.orderByAsc("sort_order");
        return paperQuestionMapper.selectList(wrapper);
    }

    private Map<String, Question> getQuestionMap(List<PaperQuestion> paperQuestions) {
        List<String> questionIds = paperQuestions.stream()
                .map(PaperQuestion::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            return new HashMap<>();
        }
        return questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, StudentPaperDetail> getDetailMap(Long recordId) {
        QueryWrapper<StudentPaperDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("record_id", recordId);
        List<StudentPaperDetail> details = studentPaperDetailMapper.selectList(wrapper);
        return details.stream()
                .collect(Collectors.toMap(StudentPaperDetail::getQuestionId, Function.identity(), (a, b) -> a));
    }

    private void upsertStudentPaperDetail(Long recordId, String questionId, String studentAnswer, Boolean isCorrect, Integer gainedScore) {
        QueryWrapper<StudentPaperDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("record_id", recordId);
        wrapper.eq("question_id", questionId);
        StudentPaperDetail detail = studentPaperDetailMapper.selectOne(wrapper);
        if (detail == null) {
            detail = new StudentPaperDetail();
            detail.setRecordId(recordId);
            detail.setQuestionId(questionId);
        }
        detail.setStudentAnswer(studentAnswer);
        detail.setIsCorrect(isCorrect);
        detail.setScoreGained(gainedScore);
        if (detail.getId() == null) {
            studentPaperDetailMapper.insert(detail);
        } else {
            studentPaperDetailMapper.updateById(detail);
        }
    }

    private boolean isSubjectiveQuestion(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.trim();
        return normalized.contains("简答") || normalized.contains("主观") || normalized.contains("代码");
    }

    private boolean checkObjectiveAnswer(Question question, String studentAnswer) {
        String type = question.getType() == null ? "" : question.getType();
        String standard = question.getAnswer() == null ? "" : question.getAnswer();
        String student = studentAnswer == null ? "" : studentAnswer;

        if (type.contains("多选")) {
            return normalizeMultiChoice(standard).equals(normalizeMultiChoice(student));
        }
        if (type.contains("填空")) {
            return checkFillInBlank(student, standard);
        }
        return standard.trim().equalsIgnoreCase(student.trim());
    }

    private Set<String> normalizeMultiChoice(String answer) {
        if (answer == null || answer.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private boolean checkFillInBlank(String studentAns, String standardAns) {
        if (studentAns == null || standardAns == null) {
            return false;
        }
        String[] stdArr = standardAns.split("[;；]");
        String[] stuArr = studentAns.split("[;；]");
        if (stdArr.length != stuArr.length) {
            return false;
        }
        for (int i = 0; i < stdArr.length; i++) {
            if (!stdArr[i].trim().equalsIgnoreCase(stuArr[i].trim())) {
                return false;
            }
        }
        return true;
    }

    private Long getClassIdByCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        return course == null ? null : course.getClassId();
    }
}
