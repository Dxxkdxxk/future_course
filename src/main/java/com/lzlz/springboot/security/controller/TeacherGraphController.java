package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.*;
import com.lzlz.springboot.security.service.GraphBuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
// (!!!) 路径已重构，所有课程相关的图谱操作都在这里
@RequestMapping("/api/v1/teacher/courses/{courseId}/graphs")
public class TeacherGraphController {

    @Autowired
    private GraphBuildService graphBuildService;

    // (!!!)
    // (!!!) 这是您需要的新接口 (Step 1: 发现) (!!!)
    // (!!!)
    /**
     * 获取指定课程下的所有图谱列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GraphInfoResponse>>> getGraphsForCourse(@PathVariable long courseId) {

        List<GraphInfoResponse> response = graphBuildService.getGraphsByCourse(courseId);
        // (!!!) 成功时返回 200 OK 和 列表
        // (如果列表为空，它会正确返回一个空数组 [])
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // (!!!)
    // (!!!) 这是您现有的上传接口 (!!!)
    // (!!!) 路径变为: POST /api/v1/teacher/courses/{courseId}/graphs/upload
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<GraphBuildResponse>> uploadAndBuildGraph(
            @PathVariable long courseId,
            @RequestParam("graphName") String graphName,
            @RequestParam("file") MultipartFile file) {

        GraphBuildResponse response = graphBuildService.buildGraphFromDocument(courseId,graphName, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // (!!!)
    // (!!!) 这是您现有的获取详情接口 (Step 2: 加载) (!!!)
    // (!!!) 路径变为: GET /api/v1/teacher/courses/{courseId}/graphs/{graphId}
    // (注意: 这个接口现在在逻辑上可以不使用 courseId，但保留它可以用于权限校验)
    @GetMapping("/{graphId}")
    public ResponseEntity<ApiResponse<GraphBuildResponse>> getGraphDetails(
            @PathVariable long courseId, // 可用于权限校验
            @PathVariable long graphId) {

        GraphBuildResponse response = graphBuildService.getGraphDetails(graphId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    /**
     * 在指定图谱中创建新节点
     * 路径: POST /api/v1/teacher/courses/{courseId}/graphs/{graphId}/nodes
     */
    @PostMapping("/{graphId}/nodes")
    public
    ResponseEntity<ApiResponse<GraphNode>> createNode(
            @PathVariable long courseId, // 可用于权限校验
            @PathVariable long graphId,
            @RequestBody CreateNodeRequest request) {

        // 1. 调用服务层
        // (注意: 服务层不需要 courseId，它只关心 graphId)
        GraphNode newNode = graphBuildService.createNode(graphId, request);

        // 2. 返回 201 Created
        // 我们将使用 ApiResponse.created() (下一步添加)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(newNode));
    }


    /**
     * 更新节点属性 (名称/描述)
     * 路径: PUT /api/v1/teacher/courses/{courseId}/graphs/{graphId}/nodes/{nodeId}
     */
    @PutMapping("/{graphId}/nodes/{nodeId}")
    public
    ResponseEntity<ApiResponse<Object>> updateNode(
            @PathVariable long courseId, // 可用于权限校验
            @PathVariable long graphId,
            @PathVariable String nodeId,
            @RequestBody UpdateNodeRequest request) {

        // 1. 调用服务层
        graphBuildService.updateNode(graphId, nodeId, request);

        // 2. (!!!) 返回你要求的 "200 OK" 和 "图谱已更新"
        // (ApiResponse.success() 不带数据)
        return ResponseEntity.ok(ApiResponse.success()); //
    }


    /**
     * 删除节点
     * 路径: DELETE .../graphs/{graphId}/nodes/{nodeId}
     */
    @DeleteMapping("/{graphId}/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<Object>> deleteNode(
            @PathVariable long courseId, // 路径参数必须占位，虽然目前不用
            @PathVariable long graphId,
            @PathVariable String nodeId) {

        graphBuildService.deleteNode(graphId, nodeId);

        // 返回 200 OK (ApiResponse.success() code 为 200)
        return ResponseEntity.ok(ApiResponse.success());
    }


    /**
     * 新增关系 (边)
     * 路径: POST /api/v1/teacher/courses/{courseId}/graphs/{graphId}/edges
     */
    @PostMapping("/{graphId}/edges")
    public ResponseEntity<ApiResponse<GraphEdge>> createEdge(
            @PathVariable long courseId,
            @PathVariable long graphId,
            @RequestBody CreateEdgeRequest request) {

        // 调用 Service
        GraphEdge newEdge = graphBuildService.createEdge(graphId, request);

        // 返回 201 Created 和 数据
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(newEdge));
    }


    /**
     * 删除关系 (边)
     * 路径: DELETE /api/v1/teacher/courses/{courseId}/graphs/{graphId}/edges/{edgeId}
     */
    @DeleteMapping("/{graphId}/edges/{edgeId}")
    public ResponseEntity<ApiResponse<Object>> deleteEdge(
            @PathVariable long courseId, // 占位，用于权限路径完整性
            @PathVariable long graphId,
            @PathVariable String edgeId) {

        graphBuildService.deleteEdge(graphId, edgeId);

        // 返回 200 OK
        return ResponseEntity.ok(ApiResponse.success());
    }


    /**
     * 更新关系 (边)
     * 路径: PUT /api/v1/teacher/courses/{courseId}/graphs/{graphId}/edges/{edgeId}
     */
    @PutMapping("/{graphId}/edges/{edgeId}")
    public ResponseEntity<ApiResponse<GraphEdge>> updateEdge(
            @PathVariable long courseId,
            @PathVariable long graphId,
            @PathVariable String edgeId,
            @RequestBody UpdateEdgeRequest request) {

        GraphEdge updatedEdge = graphBuildService.updateEdge(graphId, edgeId, request);

        return ResponseEntity.ok(ApiResponse.success(updatedEdge));
    }
}