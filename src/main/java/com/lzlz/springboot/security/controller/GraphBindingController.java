package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.NodeBindingDto;
import com.lzlz.springboot.security.service.GraphLearningProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/courses/{courseId}/graphs/{graphId}/nodes/{nodeId}/bindings")
public class GraphBindingController {

    private final GraphLearningProgressService graphLearningProgressService;

    public GraphBindingController(GraphLearningProgressService graphLearningProgressService) {
        this.graphLearningProgressService = graphLearningProgressService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> bindTask(@PathVariable Long courseId,
                                                      @PathVariable Long graphId,
                                                      @PathVariable String nodeId,
                                                      @RequestBody NodeBindingDto.UpsertRequest request) {
        graphLearningProgressService.bindNodeTask(courseId, graphId, nodeId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NodeBindingDto.BindingListResponse>> listBindings(@PathVariable Long courseId,
                                                                                         @PathVariable Long graphId,
                                                                                         @PathVariable String nodeId) {
        NodeBindingDto.BindingListResponse data = graphLearningProgressService.listNodeBindings(courseId, graphId, nodeId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeBinding(@PathVariable Long courseId,
                                                           @PathVariable Long graphId,
                                                           @PathVariable String nodeId,
                                                           @RequestBody NodeBindingDto.RemoveRequest request) {
        graphLearningProgressService.removeNodeBinding(courseId, graphId, nodeId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }
}

