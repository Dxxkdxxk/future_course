package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.StudentHomeworkDetailDto;
import com.lzlz.springboot.security.entity.Homework; // 假设你有这个实体

import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.HomeworkService;
import com.lzlz.springboot.security.service.HomeworkSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/course/{courseId}/homework")
public class StudentHomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @Autowired
    private HomeworkSubmissionService submissionService;

    /**
     * 提交作业 (支持文件上传)
     * POST /api/v1/student/course/{courseId}/homework/{homeworkId}/submit
     * Content-Type: multipart/form-data
     */
    @PostMapping("/{homeworkId}/submit")
    public ApiResponse<Void> submitHomework(
            @PathVariable Long courseId,
            @PathVariable Long homeworkId,
            // [修改点] 改为数组接收多个文件
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "content", required = false) String content,
            @AuthenticationPrincipal User user
    ) {
        submissionService.submitHomework(courseId, homeworkId, user.getId(), files, content);
        return ApiResponse.success(null);
    }

    /**
     * 获取作业列表 (之前实现的)
     */
    @GetMapping() // 请核对您的具体路径
    // [修改点 1]：返回类型泛型改为 StudentHomeworkDetailDto
    public ApiResponse<List<StudentHomeworkDetailDto>> getHomeworkList(@PathVariable
            Long courseId,
            @AuthenticationPrincipal
            User user) {
        // [修改点 2]：接收变量的类型也要改为 StudentHomeworkDetailDto
        List<StudentHomeworkDetailDto> list = homeworkService.getHomeworkListForStudent(courseId, user.getId().longValue());

        return ApiResponse.success(list);
    }

    /**
     * [新增] 获取作业详情 (学生端)
     * GET /api/v1/student/course/{courseId}/homework/{homeworkId}
     */
    @GetMapping("/{homeworkId}")
    public ApiResponse<StudentHomeworkDetailDto> getHomeworkDetail(
            @PathVariable Long courseId,
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal User user
    ) {
        StudentHomeworkDetailDto detail = homeworkService.getHomeworkDetailForStudent(
                courseId,
                homeworkId,
                user.getId().longValue()
        );
        return ApiResponse.success(detail);
    }
}