package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.GraphBuildResponse;
import com.lzlz.springboot.security.dto.GraphInfoResponse;
import com.lzlz.springboot.security.service.GraphBuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    /**
     * 1. 获取课程下的所有图谱列表
     * 路径: GET /api/v1/student/courses/{courseId}/graphs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GraphInfoResponse>>> getGraphsForCourse(@PathVariable long courseId) {

        // 复用 Service 逻辑
        List<GraphInfoResponse> list = graphBuildService.getGraphsByCourse(courseId);

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 2. 获取单个图谱详情 (之前已添加)
     * 路径: GET /api/v1/student/courses/{courseId}/graphs/{graphId}
     */
    @GetMapping("/{graphId}")
    public ResponseEntity<ApiResponse<GraphBuildResponse>> getGraphDetails(
            @PathVariable long courseId,
            @PathVariable long graphId) {

        GraphBuildResponse response = graphBuildService.getGraphDetails(graphId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}