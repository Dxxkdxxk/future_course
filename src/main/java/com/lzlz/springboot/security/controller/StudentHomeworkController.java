package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/course/{courseId}/homework")
public class StudentHomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    /**
     * 获取学生端的作业列表
     * GET /api/v1/student/course/{courseId}/homework
     * 逻辑：仅返回状态为“已发布”的作业
     */
    @GetMapping
    public ApiResponse<List<Homework>> getHomeworkList(@PathVariable Long courseId) {
        List<Homework> list = homeworkService.getHomeworkListForStudent(courseId);
        return ApiResponse.success(list);
    }
}