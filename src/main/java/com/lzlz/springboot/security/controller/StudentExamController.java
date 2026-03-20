package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.ExamFunctionDto;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.CurrentUserResolver;
import com.lzlz.springboot.security.service.ExamFunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/exam")
public class StudentExamController {

    @Autowired
    private ExamFunctionService examFunctionService;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<List<ExamFunctionDto.StudentTaskView>>> getMyTasks(
            @RequestParam Long courseId,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        List<ExamFunctionDto.StudentTaskView> list =
                examFunctionService.getStudentTasks(courseId, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(200, "success", list));
    }

    @GetMapping("/{taskId}/paper")
    public ResponseEntity<ApiResponse<ExamFunctionDto.PaperView>> getPaperContent(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        ExamFunctionDto.PaperView view = examFunctionService.getPaperContentByTask(taskId, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(200, "success", view));
    }

    @PostMapping("/{taskId}/submit")
    public ResponseEntity<ApiResponse<ExamFunctionDto.SubmitResult>> submitPaper(
            @PathVariable Long taskId,
            @RequestBody ExamFunctionDto.SubmitRequest request,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        ExamFunctionDto.SubmitResult result =
                examFunctionService.submitPaper(taskId, currentUser.getId(), request);
        return ResponseEntity.ok(new ApiResponse<>(200, "success", result));
    }

    @GetMapping("/{taskId}/my-result")
    public ResponseEntity<ApiResponse<ExamFunctionDto.StudentResultView>> getMyExamResult(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        ExamFunctionDto.StudentResultView view =
                examFunctionService.getStudentExamResult(taskId, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(200, "success", view));
    }
}
