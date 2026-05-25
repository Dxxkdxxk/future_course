package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.AbilityEvaluationResponse;
import com.lzlz.springboot.security.dto.KnowledgeEvaluationResponse;
import com.lzlz.springboot.security.dto.RadarChartResponse;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.CurrentUserResolver;
import com.lzlz.springboot.security.service.StudentEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/courses/{courseId}")
public class StudentEvaluationController {
    private final CurrentUserResolver currentUserResolver;
    private final StudentEvaluationService studentEvaluationService;

    public StudentEvaluationController(CurrentUserResolver currentUserResolver,
                                       StudentEvaluationService studentEvaluationService) {
        this.currentUserResolver = currentUserResolver;
        this.studentEvaluationService = studentEvaluationService;
    }

    @GetMapping("/knowledge-evaluation")
    public ResponseEntity<ApiResponse<KnowledgeEvaluationResponse>> getKnowledgeEvaluation(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long graphId,
            @RequestParam(defaultValue = "true") Boolean isLatest,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        KnowledgeEvaluationResponse data = studentEvaluationService.getKnowledgeEvaluation(
                currentUser.getId(), courseId, graphId, isLatest);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/ability-evaluation")
    public ResponseEntity<ApiResponse<AbilityEvaluationResponse>> getAbilityEvaluation(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long graphId,
            @RequestParam(defaultValue = "true") Boolean isLatest,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        AbilityEvaluationResponse data = studentEvaluationService.getAbilityEvaluation(
                currentUser.getId(), courseId, graphId, isLatest);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/radar-chart")
    public ResponseEntity<ApiResponse<RadarChartResponse>> getRadarChart(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long graphId,
            @RequestParam(defaultValue = "KNOWLEDGE") String chartType,
            @RequestParam(defaultValue = "false") Boolean refresh,
            @AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        RadarChartResponse data = studentEvaluationService.getRadarChart(
                currentUser.getId(), courseId, graphId, chartType, refresh);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
