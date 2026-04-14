package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.StudentHomeworkDetailDto;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.CurrentUserResolver;
import com.lzlz.springboot.security.service.HomeworkService;
import com.lzlz.springboot.security.service.HomeworkSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/course/{courseId}/homework")
public class StudentHomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @Autowired
    private HomeworkSubmissionService submissionService;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @PostMapping("/{homeworkId}/submit")
    public ApiResponse<Void> submitHomework(
            @PathVariable Long courseId,
            @PathVariable Long homeworkId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "content", required = false) String content,
            @AuthenticationPrincipal User user
    ) {
        User currentUser = currentUserResolver.requireUser(user);
        submissionService.submitHomework(courseId, homeworkId, currentUser.getId(), files, content);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<StudentHomeworkDetailDto>> getHomeworkList(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        List<StudentHomeworkDetailDto> list =
                homeworkService.getHomeworkListForStudent(courseId, currentUser.getId().longValue());
        return ApiResponse.success(list);
    }

    @GetMapping("/{homeworkId}")
    public ApiResponse<StudentHomeworkDetailDto> getHomeworkDetail(
            @PathVariable Long courseId,
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal User user
    ) {
        User currentUser = currentUserResolver.requireUser(user);
        StudentHomeworkDetailDto detail = homeworkService.getHomeworkDetailForStudent(
                courseId,
                homeworkId,
                currentUser.getId().longValue()
        );
        return ApiResponse.success(detail);
    }
}
