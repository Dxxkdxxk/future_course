package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.ExamFunctionDto;

import java.util.List;

public interface ExamFunctionService {
    /**
     * 发布测试：组卷 + 创建任务
     */
    Long publishExam(ExamFunctionDto.PublishRequest request, Integer teacherId);

    // [新增] 获取任务列表
    List<ExamFunctionDto.TaskSummary> getTeacherTaskList(Long classId);

    /**
     * [新增] 获取学生视角的任务列表
     */
    List<ExamFunctionDto.StudentTaskView> getStudentTasks(Long classId, Integer studentId);

    ExamFunctionDto.PaperView getPaperContentByTask(Long taskId, Integer studentId);

    /**
     * [新增] 学生提交试卷
     */
    ExamFunctionDto.SubmitResult submitPaper(Long taskId, Integer studentId, ExamFunctionDto.SubmitRequest request);

    /**
     * [新增] 获取测试提交列表
     */
    List<ExamFunctionDto.StudentSubmissionDto> getTaskSubmissions(Long taskId);

    /**
     * [新增] 获取阅卷详情
     */
    ExamFunctionDto.GradingView getSubmissionForGrading(Long recordId);

    /**
     * [新增] 教师提交人工评分
     */
    void gradeSubjectiveQuestions(Long recordId, ExamFunctionDto.GradeRequest request);

    /**
     * [新增] 获取学生考试结果
     */
    ExamFunctionDto.StudentResultView getStudentExamResult(Long taskId, Integer studentId);
}