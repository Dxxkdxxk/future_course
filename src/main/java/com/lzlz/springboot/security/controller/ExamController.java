package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.ExamDto;
import com.lzlz.springboot.security.security.User;
import com.lzlz.springboot.security.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ExamController {

    @Autowired
    private ExamService examService;

    /**
     * 1. [教师] 创建试卷 (组卷)
     * POST /api/v1/course/{courseId}/papers
     */
    @PostMapping("/course/{courseId}/papers")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createPaper(
            @PathVariable Long courseId,
            @RequestBody ExamDto.CreatePaperRequest request) {

        Long paperId = examService.createPaper(courseId, request);
        return ResponseEntity.ok(new ApiResponse<>(200, "试卷创建成功", Map.of("paperId", paperId)));
    }

    /**
     * 2. [教师] 发布试卷 (绑定到章节)
     * POST /api/v1/chapters/{chapterId}/bind-paper
     */
    @PostMapping("/chapters/{chapterId}/bind-paper")
    public ResponseEntity<ApiResponse<Void>> bindPaper(
            @PathVariable Long chapterId,
            @RequestBody ExamDto.PublishRequest request) {
        examService.publishToChapter(chapterId, request.getPaperId());
        return ResponseEntity.ok(new ApiResponse<>(200, "发布成功", null));
    }

    /**
     * 3. [学生] 获取章节测试内容
     * GET /api/v1/chapters/{chapterId}/exam
     */
    @GetMapping("/chapters/{chapterId}/exam")
    public ResponseEntity<ApiResponse<ExamDto.PaperView>> getExam(
            @PathVariable Long chapterId) {

        ExamDto.PaperView view = examService.getPaperForStudent(chapterId);
        return ResponseEntity.ok(new ApiResponse<>(200, "获取成功", view));
    }

    /**
     * 4. [学生] 提交试卷
     * POST /api/v1/chapters/{chapterId}/exam/submit
     */
    @PostMapping("/chapters/{chapterId}/exam/submit")
    public ResponseEntity<ApiResponse<ExamDto.ResultView>> submitExam(
            @PathVariable Long chapterId,
            @RequestBody ExamDto.SubmitRequest request,
            @AuthenticationPrincipal User user) { // 获取当前登录用户

        Long studentId = Long.valueOf(user.getId());
        ExamDto.ResultView result = examService.submitExam(studentId, chapterId, request);
        return ResponseEntity.ok(new ApiResponse<>(200, "交卷成功", result));
    }
}