package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.ExamFunctionDto;

import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.ExamFunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/exam")
public class TeacherExamController {

    @Autowired
    private ExamFunctionService examFunctionService;

    /**
     * [教师] 直接选题并发布测试给班级
     * URL: POST /api/v1/teacher/exam/publish
     */
    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<Long>> publishExam(
            @RequestBody ExamFunctionDto.PublishRequest request,
            @AuthenticationPrincipal User user) { // 获取当前登录教师ID

        // 执行发布逻辑，返回生成的任务ID (taskId)
        Long taskId = examFunctionService.publishExam(request, user.getId());

        return ResponseEntity.ok(new ApiResponse<>(200, "测试发布成功", taskId));
    }

    /**
     * [新增] 获取某班级的测试任务列表
     * GET /api/v1/teacher/exam/tasks?classId=101
     */
    @GetMapping("/tasks")
    public
    ResponseEntity<ApiResponse<List<ExamFunctionDto.TaskSummary>>> getClassTasks(@RequestParam Long classId) {

        List<ExamFunctionDto.TaskSummary> list = examFunctionService.getTeacherTaskList(classId);
        return ResponseEntity.ok(new ApiResponse<>(200, "获取成功", list));
    }


    /**
     * [新增] 获取某测试的提交情况列表
     * GET /api/v1/teacher/exam/{taskId}/submissions
     */
    @GetMapping("/{taskId}/submissions")
    public ResponseEntity<ApiResponse<List<ExamFunctionDto.StudentSubmissionDto>>> getSubmissions(
            @PathVariable Long taskId) {

        List<ExamFunctionDto.StudentSubmissionDto> list =
                examFunctionService.getTaskSubmissions(taskId);

        return ResponseEntity.ok(new ApiResponse<>(200, "获取成功", list));
    }

    /**
     * [新增] 获取单个学生的答卷详情 (用于批改)
     * GET /api/v1/teacher/exam/submission/{recordId}/detail
     */
    @GetMapping("/submission/{recordId}/detail")
    public ResponseEntity<ApiResponse<ExamFunctionDto.GradingView>> getSubmissionForGrading(
            @PathVariable Long recordId) {

        ExamFunctionDto.GradingView view =
                examFunctionService.getSubmissionForGrading(recordId);

        return ResponseEntity.ok(new ApiResponse<>(200, "获取成功", view));
    }


    /**
     * [新增] 提交人工评分
     * POST /api/v1/teacher/exam/submission/{recordId}/grade
     */
    @PostMapping("/submission/{recordId}/grade")
    public ResponseEntity<ApiResponse<Void>> gradeSubjectiveQuestions(
            @PathVariable Long recordId,
            @RequestBody ExamFunctionDto.GradeRequest request) {

        examFunctionService.gradeSubjectiveQuestions(recordId, request);

        return ResponseEntity.ok(new ApiResponse<>(200, "批改保存成功", null));
    }
}