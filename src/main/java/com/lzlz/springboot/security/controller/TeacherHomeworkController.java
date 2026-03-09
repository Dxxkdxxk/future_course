package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.entity.Homework; // 引入实体
import com.lzlz.springboot.security.service.HomeworkService;
import com.lzlz.springboot.security.dto.StudentSubmissionDto;
import com.lzlz.springboot.security.dto.GradeSubmissionRequest;
import com.lzlz.springboot.security.service.HomeworkSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List; // 引入 List

@RestController
@RequestMapping("/api/v1/teacher/course/{courseId}/homework")
public class TeacherHomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @Autowired
    private HomeworkSubmissionService submissionService; // 使用新服务

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
        HomeworkDetailResponse detail = homeworkService.getHomeworkDetailForTeacher(courseId,homeworkId);
        return ApiResponse.success(detail);
    }


    /**
     * 获取作业提交列表 (包含文件链接)
     * GET /api/v1/teacher/course/{courseId}/homework/{homeworkId}/submissions
     */
    @GetMapping("/{homeworkId}/submissions")
    public
    ApiResponse<List<StudentSubmissionDto>> getHomeworkSubmissions(@PathVariable Long courseId, @PathVariable Long homeworkId) {
        List<StudentSubmissionDto> list = submissionService.getHomeworkSubmissionList(courseId, homeworkId);
        return ApiResponse.success(list);
    }


    @GetMapping("/submission/{submissionId}")
    public ApiResponse<StudentSubmissionDto> getSubmissionDetail(
                    @PathVariable
                    Long courseId,
                    @PathVariable
                    Long submissionId
    ) {
        // [修改] 传入 courseId 进行校验
        StudentSubmissionDto detail = submissionService.getSubmissionDetail(courseId, submissionId);
        return ApiResponse.success(detail);
    }


    /**
     * [新增] 教师批改作业接口
     * POST /api/v1/teacher/course/{courseId}/homework/submission/{submissionId}/grade
     */
    @PostMapping("/submission/{submissionId}/grade")
    public ApiResponse<Void> gradeSubmission
    (
            @PathVariable
            Long courseId,       // 用于校验
            @PathVariable
            Long submissionId,   // 目标提交记录
            @RequestBody
            GradeSubmissionRequest request // 分数和评语
    )
    {
        submissionService.gradeSubmission(
                courseId,
                submissionId,
                request.getScore(),
                request.getComment()
        );

        return ApiResponse.success(null
        );
    }
}