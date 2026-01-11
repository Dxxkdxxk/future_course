package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher/course/{courseId}/homework")
public class TeacherHomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    /**
     * 发布作业
     * POST /api/v1/teacher/course/{courseId}/homework
     */
    @PostMapping
    public ApiResponse<Void> publishHomework(
            @PathVariable Long courseId,
            @RequestBody CreateHomeworkRequest request) {

        // 调用 Service 执行发布逻辑
        homeworkService.createHomework(courseId, request);

        return ApiResponse.success(null);
    }

    /**
     * 获取作业详情（包含题目列表）
     * GET /api/v1/teacher/course/{courseId}/homework/{homeworkId}
     */
    @GetMapping("/{homeworkId}")
    public ApiResponse<HomeworkDetailResponse> getHomeworkDetail(@PathVariable Long courseId, @PathVariable Long homeworkId) {

        // 调用 Service 获取详情
        HomeworkDetailResponse detail = homeworkService.getHomeworkDetailForTeacher(homeworkId);
        return ApiResponse.success(detail);
    }
}