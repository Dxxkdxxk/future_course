package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.entity.Homework; // 引入实体
import com.lzlz.springboot.security.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List; // 引入 List

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
        homeworkService.createHomework(courseId, request);
        return ApiResponse.success(null);
    }

    /**
     * (新增) 获取课程作业列表
     * GET /api/v1/teacher/course/{courseId}/homework
     */
    @GetMapping
    public ApiResponse<List<Homework>> getHomeworkList(@PathVariable Long courseId) {
        // 调用 Service 查询列表
        List<Homework> list = homeworkService.getHomeworkList(courseId);
        return ApiResponse.success(list);
    }

    /**
     * 获取作业详情（包含题目列表）
     * GET /api/v1/teacher/course/{courseId}/homework/{homeworkId}
     */
    @GetMapping("/{homeworkId}")
    public ApiResponse<HomeworkDetailResponse> getHomeworkDetail(@PathVariable Long courseId, @PathVariable Long homeworkId) {
        HomeworkDetailResponse detail = homeworkService.getHomeworkDetailForTeacher(homeworkId);
        return ApiResponse.success(detail);
    }
}