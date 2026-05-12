package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.VideoProgressDto;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.CurrentUserResolver;
import com.lzlz.springboot.security.service.GraphLearningProgressService;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/courses/{courseId}/graphs/{graphId}/nodes/{nodeId}")
public class StudentGraphProgressController {

    private final StudentCourseAccessService studentCourseAccessService;
    private final CurrentUserResolver currentUserResolver;
    private final GraphLearningProgressService graphLearningProgressService;

    public StudentGraphProgressController(StudentCourseAccessService studentCourseAccessService,
                                          CurrentUserResolver currentUserResolver,
                                          GraphLearningProgressService graphLearningProgressService) {
        this.studentCourseAccessService = studentCourseAccessService;
        this.currentUserResolver = currentUserResolver;
        this.graphLearningProgressService = graphLearningProgressService;
    }

    @PostMapping("/video-progress")
    public ResponseEntity<ApiResponse<VideoProgressDto.ReportResponse>> reportVideoProgress(@PathVariable Long courseId,
                                                                                             @PathVariable Long graphId,
                                                                                             @PathVariable String nodeId,
                                                                                             @AuthenticationPrincipal User user,
                                                                                             @RequestBody VideoProgressDto.ReportRequest request) {
        User currentUser = currentUserResolver.requireUser(user);
        studentCourseAccessService.checkGraphAccess(currentUser.getId(), courseId, graphId);
        VideoProgressDto.ReportResponse response = graphLearningProgressService.reportVideoProgress(
                courseId, graphId, nodeId, currentUser.getId(), request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

