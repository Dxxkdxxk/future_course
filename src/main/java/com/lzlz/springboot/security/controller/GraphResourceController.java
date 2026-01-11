package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.GraphResourceDto;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.repository.GraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// (!!!) 路径已修正：对齐 TeacherGraphController
@RequestMapping("/api/v1/teacher/courses/{courseId}/graphs/{graphId}")
public class GraphResourceController {

    @Autowired
    private GraphRepository graphRepository;

    /**
     * 挂载资源
     */
    @PostMapping("/nodes/{nodeId}/resources")
    public
    ResponseEntity<ApiResponse<Void>> bindResource(@PathVariable Long courseId, @PathVariable Long graphId, @PathVariable String nodeId, @RequestBody GraphResourceDto.BindRequest request) {

        // (!!!) 1. 前置校验：节点是否存在
        boolean exists = graphRepository.checkNodeExists(graphId, nodeId);
        if (!exists) {
            // 返回 404
            throw new ResourceNotFoundException("挂载失败：目标知识点不存在");
        }

        // 2. 执行挂载
        graphRepository.bindResource(graphId, nodeId, request);
        return ResponseEntity.ok(new ApiResponse<>(200, "挂载成功", null));
    }

    /**
     * 2. 获取资源列表
     * Path: /nodes/{nodeId}/resources
     */
    @GetMapping("/nodes/{nodeId}/resources")
    public ResponseEntity<ApiResponse<List<GraphResourceDto.ResourceView>>> getResources(
            @PathVariable Long courseId,
            @PathVariable Long graphId,
            @PathVariable String nodeId) {

        List<GraphResourceDto.ResourceView> list = graphRepository.getNodeResources(graphId, nodeId);
        return ResponseEntity.ok(new ApiResponse<>(200, "获取成功", list));
    }

    /**
     * 3. 删除资源
     * Path: /resources/{resourceId}
     * Full: /api/v1/teacher/courses/{courseId}/graphs/{graphId}/resources/{resourceId}
     */
    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable Long courseId,
            @PathVariable Long graphId,
            @PathVariable String resourceId) {

        graphRepository.deleteResource(graphId, resourceId);
        return ResponseEntity.ok(new ApiResponse<>(200, "删除成功", null));
    }
}