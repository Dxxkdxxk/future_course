package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.GradeSubmissionRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.dto.StudentSubmissionDto;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.service.HomeworkService;
import com.lzlz.springboot.security.service.HomeworkSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/course/{courseId}/homework")
public class TeacherHomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @Autowired
    private HomeworkSubmissionService submissionService;

    /**
     * Publish homework (supports attachments in multipart form).
     */
    @PostMapping
    public ApiResponse<Void> publishHomework(
            @PathVariable Long courseId,
            @ModelAttribute CreateHomeworkRequest request,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        homeworkService.createHomework(courseId, request, files);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<Homework>> getHomeworkList(@PathVariable Long courseId) {
        List<Homework> list = homeworkService.getHomeworkList(courseId);
        return ApiResponse.success(list);
    }

    @GetMapping("/{homeworkId}")
    public ApiResponse<HomeworkDetailResponse> getHomeworkDetail(@PathVariable Long courseId, @PathVariable Long homeworkId) {
        HomeworkDetailResponse detail = homeworkService.getHomeworkDetailForTeacher(courseId, homeworkId);
        return ApiResponse.success(detail);
    }

    @GetMapping("/{homeworkId}/submissions")
    public ApiResponse<List<StudentSubmissionDto>> getHomeworkSubmissions(@PathVariable Long courseId, @PathVariable Long homeworkId) {
        List<StudentSubmissionDto> list = submissionService.getHomeworkSubmissionList(courseId, homeworkId);
        return ApiResponse.success(list);
    }

    @GetMapping("/submission/{submissionId}")
    public ApiResponse<StudentSubmissionDto> getSubmissionDetail(
            @PathVariable Long courseId,
            @PathVariable Long submissionId) {
        StudentSubmissionDto detail = submissionService.getSubmissionDetail(courseId, submissionId);
        return ApiResponse.success(detail);
    }

    @PostMapping("/submission/{submissionId}/grade")
    public ApiResponse<Void> gradeSubmission(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @RequestBody GradeSubmissionRequest request) {
        submissionService.gradeSubmission(courseId, submissionId, request.getScore(), request.getComment());
        return ApiResponse.success(null);
    }
}
