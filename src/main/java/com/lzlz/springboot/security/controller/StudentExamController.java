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
@RequestMapping("/api/v1/student/exam")
public class StudentExamController {

    @Autowired
    private ExamFunctionService examFunctionService;

    /**
     * [学生] 获取我的测试任务列表
     * GET /api/v1/student/exam/my-tasks?classId=101
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<List<ExamFunctionDto.StudentTaskView>>> getMyTasks(
            @RequestParam Long classId,
            @AuthenticationPrincipal User user) {

        List<ExamFunctionDto.StudentTaskView> list =
                examFunctionService.getStudentTasks(classId, user.getId());

        return ResponseEntity.ok(new ApiResponse<>(200, "获取成功", list));
    }


    /**
     * [新增] 获取试卷内容 (点击"开始考试")
     * GET /api/v1/student/exam/{taskId}/paper
     */
    @GetMapping("/{taskId}/paper")
    public
    ResponseEntity<ApiResponse<ExamFunctionDto.PaperView>> getPaperContent(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        // 调用 Service
        ExamFunctionDto.PaperView view = examFunctionService.getPaperContentByTask(taskId, user.getId());

        return ResponseEntity.ok(new ApiResponse<>(200, "试卷加载成功", view));
    }


    /**
     * [新增] 提交试卷
     * POST /api/v1/student/exam/{taskId}/submit
     */
    @PostMapping("/{taskId}/submit")
    public ResponseEntity<ApiResponse<ExamFunctionDto.SubmitResult>> submitPaper(
            @PathVariable Long taskId,
            @RequestBody ExamFunctionDto.SubmitRequest request,
            @AuthenticationPrincipal User user) {

        ExamFunctionDto.SubmitResult result =
                examFunctionService.submitPaper(taskId, user.getId(), request);

        return ResponseEntity.ok(new ApiResponse<>(200, "交卷成功", result));
    }


    /**
     * [新增] 查看考试结果 (含解析)
     * GET /api/v1/student/exam/{taskId}/my-result
     */
    @GetMapping("/{taskId}/my-result")
    public ResponseEntity<ApiResponse<ExamFunctionDto.StudentResultView>> getMyExamResult(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        ExamFunctionDto.StudentResultView view =
                examFunctionService.getStudentExamResult(taskId, user.getId());

        return ResponseEntity.ok(new ApiResponse<>(200, "获取成功", view));
    }
}