package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.*;
import com.lzlz.springboot.security.entity.GraphMetadata;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.GraphMetadataMapper;
import com.lzlz.springboot.security.repository.GraphRepository;
import com.lzlz.springboot.security.service.GraphBuildService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphBuildServiceImpl implements GraphBuildService {

    @Autowired
    private GraphMetadataMapper metadataMapper;

    @Autowired
    private GraphRepository graphRepository;

    // (!!!) 这是 .xlsx 文件的正确 Content Type
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // (!!!) 修改点：从配置文件读取，默认值为 5
    @Value("${graph.limit.max-depth:5}")
    private int maxDepth;

    // (!!!) 修改点：从配置文件读取，默认值为 2000
    @Value("${graph.limit.max-nodes:2000}")
    private int maxNodeCount;


    // 定义每一层对应的 Label (根据列索引 0-4)
    private static final String[] LEVEL_LABELS = {
            "知识点", // 第0列：模块/章
            "知识单元",   // 第1列：单元/节
            "知识单元",  // 第2列：知识点
            "知识单元",  // 第3列：知识点
            "知识单元"   // 第4列：知识点
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraphBuildResponse buildGraphFromDocument(long courseId, String graphName, MultipartFile file) {

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        // 核心映射：名称 -> UUID (用于去重和查找父节点)
        Map<String, String> nameToUuidMap = new HashMap<>();
        // 辅助集合：用于去重边 (防止重复创建 A->B 的关系)
        Set<String> existingEdges = new HashSet<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // ==========================================
            // Step 1: 解析【知识树】Sheet (层级结构)
            // ==========================================
            Sheet treeSheet = workbook.getSheet("知识树");
            if (treeSheet == null) throw new RuntimeException("未找到[知识树]工作表");

            // 从第2行开始 (Row 1)
            for (int i = 2; i <= treeSheet.getLastRowNum(); i++) {
                Row row = treeSheet.getRow(i);
                if (row == null) continue;

                // 遍历 5 个可能的层级 (A, B, C, D, E)
                String parentId = null; // 用于记录当前行“上一级”节点的ID

                for (int col = 0; col < maxDepth; col++) {
                    String name = getCellValue(row.getCell(col));
                    if (name.isEmpty()) break;

                    String currentId;

                    if (nameToUuidMap.containsKey(name)) {
                        currentId = nameToUuidMap.get(name);
                    }
                    else {
                        // (!!!) 使用配置变量 maxNodeCount 进行熔断检查
                        if (nodes.size() >= maxNodeCount) {
                            throw new CustomGraphException(400, String.format("图谱节点数量超限！当前限制最大 %d 个节点，以保证系统流畅运行。", maxNodeCount));
                        }

                        currentId = "node-" + UUID.randomUUID().toString();
                        // 防止数组越界 (如果配置的深度超过了预设Label数组长度)
                        String label = (col < LEVEL_LABELS.length) ? LEVEL_LABELS[col] : "KnowledgePoint";

                        GraphNode node = GraphNode.builder().nodeId(currentId).name(name).label(label).description("").build();
                        nodes.add(node);
                        nameToUuidMap.put(name, currentId);
                    }

                    // 2. 关系构建逻辑 (从第二列开始)
                    if (col > 0 && parentId != null) {
                        // 唯一标识一条边: ParentID -> ChildID
                        String edgeKey = parentId + "->" + currentId;

                        if (!existingEdges.contains(edgeKey)) {
                            GraphEdge edge = GraphEdge.builder()
                                    .edgeId("edge-" + UUID.randomUUID().toString())
                                    .sourceNodeId(parentId) // 上一列的节点
                                    .targetNodeId(currentId) // 当前列的节点
                                    .relationType("contains")
                                    .build();
                            edges.add(edge);
                            existingEdges.add(edgeKey);
                        }
                    }

                    // 3. 将当前节点设为下一列的“父节点”
                    parentId = currentId;
                }
            }

            // ==========================================
            // Step 2: 解析【关系】Sheet (网状连接)
            // ==========================================
            Sheet relationSheet = workbook.getSheet("关系");
            if (relationSheet != null) {
                // 假设表头: 源节点(0), 关系类型(1), 目标节点(2)
                for (int i = 1; i <= relationSheet.getLastRowNum(); i++) {
                    Row row = relationSheet.getRow(i);
                    if (row == null) continue;

                    String sourceName = getCellValue(row.getCell(0));
                    String relType = getCellValue(row.getCell(1));
                    String targetName = getCellValue(row.getCell(2));

                    String sourceUuid = nameToUuidMap.get(sourceName);
                    String targetUuid = nameToUuidMap.get(targetName);

                    // 只有当两个节点都已存在时，才创建关系
                    if (sourceUuid != null && targetUuid != null && !relType.isEmpty()) {
                        GraphEdge customEdge = GraphEdge.builder()
                                .edgeId("edge-" + UUID.randomUUID().toString())
                                .sourceNodeId(sourceUuid)
                                .targetNodeId(targetUuid)
                                .relationType(relType)
                                .build();
                        edges.add(customEdge);
                    }
                }
            }

            // ==========================================
            // Step 3: 保存到数据库
            // ==========================================
            GraphMetadata metadata = new GraphMetadata();
            metadata.setCourseId(courseId);
            metadata.setName(graphName);
            metadata.setCreatedAt(LocalDateTime.now());
            metadataMapper.insert(metadata);

            long newGraphId = metadata.getGraphId();
            graphRepository.saveGraph(newGraphId, nodes, edges);

            return GraphBuildResponse.builder()
                    .graphId(newGraphId)
                    .nodes(nodes)
                    .edges(edges)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("图谱生成失败: " + e.getMessage());
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
//    @Override
//    @Transactional
//    public GraphBuildResponse buildGraphFromDocument(long courseId,String graphName, MultipartFile file) {
//
//        // 1. 验证文件
//        if (file.isEmpty() || file.getContentType() == null ||
//                !file.getContentType().equals(XLSX_CONTENT_TYPE)) {
//            // (!!!) 错误码 30201: 消息已更新
//            throw new CustomGraphException(30201, "文件格式不支持，请上传XLSX文件");
//        }
//
//        List<GraphNode> apiNodes = new ArrayList<>();
//        List<GraphEdge> apiEdges = new ArrayList<>();
//
//        // (!!!) 关键：我们需要用Map来处理用“名称”进行关联
//        // 1. 节点名称 -> UUID
//        Map<String, String> nodeNameToUuidMap = new HashMap<>();
//        // 2. 节点名称 -> 其父节点的名称
//        Map<String, String> childToParentNameMap = new HashMap<>();
//
//
//        try (InputStream is = file.getInputStream();
//             XSSFWorkbook workbook = new XSSFWorkbook(is)) { // (!!!) 使用 Apache POI
//
//            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
//
//            // (!!!) Pass 1: 遍历行，创建节点
//            for (Row row : sheet) {
//                // (!!!) 跳过前两行
//                if (row.getRowNum() < 2) {
//                    continue;
//                }
//                // (!!!) 按您的要求读取列
//                String nodeName = getCellStringValue(row.getCell(0)); // 第1列: 节点名称
//                String nodeType = getCellStringValue(row.getCell(2)); // 第3列: 节点类型
//                String parentName = getCellStringValue(row.getCell(3)); // 第4列: 上级节点名称
//
//                // 跳过无效行
//                if (nodeName == null || nodeName.isBlank() || nodeType == null || nodeType.isBlank()) {
//                    continue;
//                }
//
//                String newNodeId = UUID.randomUUID().toString();
//                nodeNameToUuidMap.put(nodeName, newNodeId);
//
//                if (parentName != null && !parentName.isBlank()) {
//                    childToParentNameMap.put(nodeName, parentName);
//                }
//
//                apiNodes.add(GraphNode.builder()
//                        .nodeId(newNodeId)
//                        .name(nodeName)
//                        .label(nodeType) // (!!!) 使用第3列的类型
//                        .description("") // (!!!) 您的XLSX中没有描述，设为空
//                        .build());
//            }
//
//            // (!!!) Pass 2: 遍历Map，创建边
//            for (Map.Entry<String, String> entry : childToParentNameMap.entrySet()) {
//                String childName = entry.getKey();
//                String parentName = entry.getValue();
//
//                // (!!!) 用名称查找UUID
//                String sourceUuid = nodeNameToUuidMap.get(parentName);
//                String targetUuid = nodeNameToUuidMap.get(childName);
//
//                // 只有父子节点都存在时才创建边
//                if (sourceUuid != null && targetUuid != null) {
//                    apiEdges.add(GraphEdge.builder()
//                            .edgeId(UUID.randomUUID().toString())
//                            .sourceNodeId(sourceUuid)
//                            .targetNodeId(targetUuid)
//                            .relationType("contains") // 默认关系
//                            .build());
//                }
//            }
//
//        } catch (Exception e) { // 捕获所有解析异常 (IO, POI, etc.)
//            // (!!!) 错误码 30202: 消息已更新
//            throw new CustomGraphException(30202, "文件已加密、损坏或XLSX解析失败: " + e.getMessage());
//        }
//
//        if (apiNodes.isEmpty()) {
//            // (!!!) 错误码 30203
//            throw new CustomGraphException(30203, "无法提取有效信息，请检查XLSX文件");
//        }
//
//        // --- Pass 3: 保存到数据库 (此后逻辑不变) ---
//
//        // 1. 准备元数据
//        GraphMetadata metadata = new GraphMetadata();
//        metadata.setCourseId(courseId);
//        // 你可以增加一个检查，防止传入的名称为空
//        if (graphName != null && !graphName.isBlank()) {
//            metadata.setName(graphName);
//        } else {
//            // 如果名称为空，你可以抛出异常，或者使用一个默认值
//            // throw new CustomGraphException(30204, "图谱名称不能为空");
//            // 或者:
//            metadata.setName("未命名图谱");
//        }
//
//        // 2. 保存到MySQL (使用MP)
//        metadataMapper.insert(metadata);
//
//        // 3. 获取MySQL生成的自增ID
//        long newGraphId = metadata.getGraphId();
//
//        // 4. 保存图谱结构到Neo4j
//        graphRepository.saveGraph(newGraphId, apiNodes, apiEdges);
//
//        // 5. 返回成功响应
//        return GraphBuildResponse.builder()
//                .graphId(newGraphId)
//                .nodes(apiNodes)
//                .edges(apiEdges)
//                .build();
//    }

    /**
     * 安全地获取单元格的字符串值 (防止 NullPointerException)
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return cell.getStringCellValue();
    }

    @Override
    public GraphBuildResponse getGraphDetails(long graphId)
    {

        // 1. (!!!) 检查 MySQL 中是否存在该图谱
        // (使用 MyBatis-Plus 提供的 selectById)
        GraphMetadata metadata = metadataMapper.selectById(graphId);
        if (metadata == null
        ) {
            // (!!!) 如果MySQL中不存在，立即抛出 404
            throw new ResourceNotFoundException("Graph metadata not found with id: " + graphId);
        }

        // 2. 从 Neo4j 获取节点和边
        Map<String, List<Map<String, Object>>> components = graphRepository.getGraphComponents(graphId);

        // 3. 将 Neo4j 的 Map 结果 转换为 DTO 列表
        List<GraphNode> nodes = components.get("nodes").stream()
                .map(map -> GraphNode.builder()
                        .nodeId((String) map.get("nodeId"))
                        .name((String) map.get("name"))
                        .description((String) map.get("description"))
                        .label((String) map.get("label"))
                        .build()).collect(Collectors.toList());

        List<GraphEdge> edges = components.get("edges").stream()
                .map(map -> GraphEdge.builder()
                        .edgeId((String) map.get("edgeId"))
                        .sourceNodeId((String) map.get("sourceNodeId"))
                        .targetNodeId((String) map.get("targetNodeId"))
                        .relationType((String) map.get("relationType"))
                        .build()).collect(Collectors.toList());

        // 4. 构建并返回最终的 DTO
        return GraphBuildResponse.builder().graphId(graphId).nodes(nodes).edges(edges).build();
    }


    @Override
    public List<GraphInfoResponse> getGraphsByCourse(long courseId)
    {

        // 1. (!!!) 使用 MyBatis-Plus 的 QueryWrapper
        // 查找 'knowledge_graphs' 表中 'course_id' 匹配的所有记录
        QueryWrapper<GraphMetadata> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId);
        queryWrapper.orderByDesc("created_at"); // 按创建时间排序

        // 2. (!!!) 查询 MySQL 数据库
        List<GraphMetadata> metadataList = metadataMapper.selectList(queryWrapper);

        // 3. (!!!) 将数据库实体 (Entity) 转换为前端 DTO
        return metadataList.stream().map(metadata -> {GraphInfoResponse dto = new GraphInfoResponse();
            dto.setGraphId(metadata.getGraphId());
            dto.setName(metadata.getName());
            dto.setCreatedAt(metadata.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GraphNode createNode(long graphId, CreateNodeRequest request)
    {
        Map<String, Object> nodeData;
        try {
            nodeData = graphRepository.createNodeAndLink(graphId, request);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch
        (Exception e) {
            // (!!!) 2. 捕获所有其他 Neo4j 异常 (e.g., 连接失败, Cypher 语法错误)
            // (这是 line 253)
            throw new RuntimeException("Failed to create node in Neo4j: " + e.getMessage(), e);
        }

        // 3. 将 Repository 返回的 Map 转换为 GraphNode DTO
        GraphNode newNode = GraphNode.builder()
                .nodeId((String) nodeData.get("nodeId"))
                .name((String) nodeData.get("name"))
                .description((String) nodeData.get("description"))
                .label((String) nodeData.get("label"))
                .build();

        // 4. 返回新创建的节点 DTO
        return newNode;
    }

    @Override
    @Transactional // (!!!) 确保操作的原子性
    public void updateNode(long graphId, String nodeId, UpdateNodeRequest request)
    {

        // 1. (!!!) [关键] 调用 Repository
        // 所有的业务逻辑 (查找, 查重, 更新) 都在 GraphRepository 的
        // updateNodeProperties 方法中，并由一个 Neo4j 事务保证
        graphRepository.updateNodeProperties(graphId, nodeId, request);

        // 2. (!!!) [重要]
        // 我们不需要检查 "ResourceNotFoundException" 或 "CustomGraphException".
        // 如果它们在 Repository 中被抛出，@Transactional 会确保事务回滚，
        // 并且你的 GlobalExceptionHandler 会自动捕获它们，
        // 并将其转换为 404 或 400 响应。
    }


    @Override
    @Transactional
    public void deleteNode(long graphId, String nodeId) {

        // 1. (!!!) TODO: 检查资源中心是否已挂载知识
        // 这里的逻辑目前空着，等资源中心模块完成后再补充
        /*
        boolean hasResources = resourceClient.checkResourcesBound(nodeId);
        if (hasResources) {
             // (!!!) 这里就是你要求的 30212
             throw new CustomGraphException(30212, "删除失败：该节点已挂载知识，请先解除挂载");
        }
        */

        // 2. 调用 Repository 执行删除
        // 注意：Repository 内部已经处理了 "是否有子节点" 的检查 (30213)
        boolean deleted = graphRepository.deleteNode(graphId, nodeId);

        // 3. 如果没删掉 (deleted == false)，说明节点不存在，抛出 404
        if (!deleted) {
            throw new ResourceNotFoundException("Node not found with id: " + nodeId);
        }
    }


    @Override
    @Transactional
    public GraphEdge createEdge(long graphId, CreateEdgeRequest request) {

        // 1. 校验输入
        if (request.getSourceNodeId().equals(request.getTargetNodeId())) {
            // 可以定义一个新的错误码，比如 30214
            throw new CustomGraphException(30214, "不能创建自环关系(源节点和目标节点相同)");
        }

        // 2. 处理默认关系类型
        String type = request.getRelationType();
        if (type == null || type.isBlank()) {
            type = "contains"; // 默认值
        }

        // 3. 调用 Repository
        Map<String, Object> edgeData = graphRepository.createEdge(
                graphId,
                request.getSourceNodeId(),
                request.getTargetNodeId(),
                type
        );

        // 4. 转换为 DTO
        return GraphEdge.builder()
                .edgeId((String) edgeData.get("edgeId"))
                .sourceNodeId((String) edgeData.get("sourceNodeId"))
                .targetNodeId((String) edgeData.get("targetNodeId"))
                .relationType((String) edgeData.get("relationType"))
                .build();
    }


    @Override
    @Transactional
    public void deleteEdge(long graphId, String edgeId) {
        // 1. 调用 Repository 执行删除
        boolean deleted = graphRepository.deleteEdge(graphId, edgeId);

        // 2. 如果没删掉 (返回 false)，说明边不存在，抛出 404
        if (!deleted) {
            throw new ResourceNotFoundException("Edge not found with id: " + edgeId);
        }
    }


    @Override
    @Transactional
    public GraphEdge updateEdge(long graphId, String edgeId, UpdateEdgeRequest request) {

        // 1. 调用 Repository
        // 这里不需要在 Service 层做 if (s.equals(t)) 的校验了，
        // 因为 Repository 层会结合数据库旧值进行更准确的校验。
        Map<String, Object> result = graphRepository.updateEdge(
                graphId,
                edgeId,
                request.getSourceNodeId(),
                request.getTargetNodeId(),
                request.getRelationType()
        );

        // 2. 处理结果
        if (result == null) {
            throw new ResourceNotFoundException("Edge not found with id: " + edgeId);
        }

        // 3. 转换为 DTO 返回
        return GraphEdge.builder()
                .edgeId((String) result.get("edgeId"))
                .sourceNodeId((String) result.get("sourceNodeId"))
                .targetNodeId((String) result.get("targetNodeId"))
                .relationType((String) result.get("relationType"))
                .build();
    }
}