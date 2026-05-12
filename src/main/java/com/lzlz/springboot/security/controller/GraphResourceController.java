package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.GraphResourceDto;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.repository.GraphRepository;
import com.lzlz.springboot.security.service.GraphLearningProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/courses/{courseId}/graphs/{graphId}")
public class GraphResourceController {

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private GraphLearningProgressService graphLearningProgressService;

    @PostMapping("/nodes/{nodeId}/resources")
    public ResponseEntity<ApiResponse<Void>> bindResource(@PathVariable Long courseId,
                                                          @PathVariable Long graphId,
                                                          @PathVariable String nodeId,
                                                          @RequestBody GraphResourceDto.BindRequest request) {
        boolean exists = graphRepository.checkNodeExists(graphId, nodeId);
        if (!exists) {
            throw new ResourceNotFoundException("Node not found in graph");
        }
        graphRepository.bindResource(graphId, nodeId, request);
        graphLearningProgressService.recalculateAllStudentsForNode(courseId, graphId, nodeId);
        return ResponseEntity.ok(new ApiResponse<>(200, "success", null));
    }

    @GetMapping("/nodes/{nodeId}/resources")
    public ResponseEntity<ApiResponse<List<GraphResourceDto.ResourceView>>> getResources(@PathVariable Long courseId,
                                                                                          @PathVariable Long graphId,
                                                                                          @PathVariable String nodeId) {
        List<GraphResourceDto.ResourceView> list = graphRepository.getNodeResources(graphId, nodeId);
        return ResponseEntity.ok(new ApiResponse<>(200, "success", list));
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long courseId,
                                                            @PathVariable Long graphId,
                                                            @PathVariable String resourceId) {
        String ownerNodeId = graphRepository.findResourceOwnerNodeId(graphId, resourceId);
        graphRepository.deleteResource(graphId, resourceId);
        if (ownerNodeId != null && !ownerNodeId.isBlank()) {
            graphLearningProgressService.recalculateAllStudentsForNode(courseId, graphId, ownerNodeId);
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "success", null));
    }
}

