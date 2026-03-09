package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface GraphBuildService {

    GraphBuildResponse buildGraphFromDocument(long courseId, String graphName, MultipartFile file);

    GraphBuildResponse getGraphDetails(long graphId);

    // (!!!)
    // (!!!) 这是您需要添加的新方法 (!!!)
    // (!!!)
    /**
     * 根据课程ID获取其下所有的图谱列表
     * @param courseId 课程ID
     * @return 图谱元信息列表 (用于前端选择)
     */
    List<GraphInfoResponse> getGraphsByCourse(long courseId);

    /**
     * 在指定图谱中创建一个新节点，并可选地将其连接到父节点
     *
     @param
     graphId 图谱ID
     *
     @param
     request 包含新节点信息和 parentId 的请求
     *
     @return
     成功创建的节点信息 (GraphNode)
     */
    GraphNode createNode(long graphId, CreateNodeRequest request);

    /**
     * 更新指定图谱中的一个节点
     *
     @param
     graphId 图谱ID (用于鉴权和定位)
     *
     @param
     nodeId 要更新的节点的ID
     *
     @param
     request 包含新名称和/或新描述的请求
     */
    void updateNode(long graphId, String nodeId, UpdateNodeRequest request);

    void deleteNode(long graphId, String nodeId);

    GraphEdge createEdge(long graphId, CreateEdgeRequest request);

    void deleteEdge(long graphId, String edgeId);

    GraphEdge updateEdge(long graphId, String edgeId, UpdateEdgeRequest request);
}