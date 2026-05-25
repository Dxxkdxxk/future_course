package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.PageResponse;
import com.lzlz.springboot.security.dto.RecommendationResponse;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.CurrentUserResolver;
import com.lzlz.springboot.security.service.StudentRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
public class StudentRecommendationController {
    private final CurrentUserResolver currentUserResolver;
    private final StudentRecommendationService studentRecommendationService;

    public StudentRecommendationController(CurrentUserResolver currentUserResolver,
                                           StudentRecommendationService studentRecommendationService) {
        this.currentUserResolver = currentUserResolver;
        this.studentRecommendationService = studentRecommendationService;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<PageResponse<RecommendationResponse>>> getRecommendations(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long graphId,
            @RequestParam(required = false) String weakPointId,
            @RequestParam(required = false) Boolean isCompleted,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        PageResponse<RecommendationResponse> data = studentRecommendationService.getRecommendations(
                currentUser.getId(), courseId, graphId, weakPointId, isCompleted, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
