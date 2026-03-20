package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.GraphBuildResponse;
import com.lzlz.springboot.security.dto.GraphInfoResponse;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.CurrentUserResolver;
import com.lzlz.springboot.security.service.GraphBuildService;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/courses/{courseId}/graphs")
public class StudentGraphController {

    @Autowired
    private GraphBuildService graphBuildService;

    @Autowired
    private StudentCourseAccessService studentCourseAccessService;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GraphInfoResponse>>> getGraphsForCourse(
            @PathVariable long courseId,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        studentCourseAccessService.checkCourseAccess(currentUser.getId(), courseId);
        List<GraphInfoResponse> list = graphBuildService.getGraphsByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{graphId}")
    public ResponseEntity<ApiResponse<GraphBuildResponse>> getGraphDetails(
            @PathVariable long courseId,
            @PathVariable long graphId,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        studentCourseAccessService.checkGraphAccess(currentUser.getId(), courseId, graphId);
        GraphBuildResponse response = graphBuildService.getGraphDetails(graphId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
