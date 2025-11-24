package com.lzlz.springboot.security.repository;

import com.lzlz.springboot.security.dto.CreateNodeRequest;
import com.lzlz.springboot.security.dto.GraphEdge;
import com.lzlz.springboot.security.dto.GraphNode;
import com.lzlz.springboot.security.dto.UpdateNodeRequest;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Neo4j 仓库
 * 【重要前提】:
 * 以下代码使用了 Neo4j 的 APOC 插件 (apoc.create.node/relationship)。
 * 请确保您的 Neo4j 数据库已正确安装 APOC 插件。
 */
@Repository
public class GraphRepository {

    @Autowired
    private Driver neo4jDriver;

    public void saveGraph(long graphId, List<GraphNode> nodes, List<GraphEdge> edges) {

        try (Session session = neo4jDriver.session()) {

            session.writeTransaction(tx -> {
                // 1. 准备节点数据
                List<Map<String, Object>> nodeMaps = nodes.stream()
                        .map(node -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("nodeId", node.getNodeId());
                            map.put("name", node.getName());
                            map.put("description", node.getDescription());
                            map.put("label", node.getLabel());
                            return map;
                        })
                        .collect(Collectors.toList());

                // 2. 批量创建节点 (使用 UNWIND 和 APOC)
                tx.run("UNWIND $nodes AS nodeProps " +
                                "CALL apoc.create.node([nodeProps.label, 'KnowledgeNode'], " +
                                "{nodeId: nodeProps.nodeId, name: nodeProps.name, description: nodeProps.description, graphId: $graphId}) " +
                                "YIELD node RETURN count(node)",
                        Map.of("nodes", nodeMaps, "graphId", graphId));

                // 3. 准备边数据
                List<Map<String, Object>> edgeMaps = edges.stream()
                        .map(edge -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("edgeId", edge.getEdgeId());
                            map.put("sourceId", edge.getSourceNodeId());
                            map.put("targetId", edge.getTargetNodeId());
                            map.put("type", edge.getRelationType());
                            return map;
                        })
                        .collect(Collectors.toList());

                // 4. 批量创建边 (使用 UNWIND 和 APOC)
                tx.run("UNWIND $edges AS edgeProps " +
                                "MATCH (source:KnowledgeNode {nodeId: edgeProps.sourceId}) " +
                                "MATCH (target:KnowledgeNode {nodeId: edgeProps.targetId}) " +
                                "CALL apoc.create.relationship(source, edgeProps.type, {edgeId: edgeProps.edgeId}, target) " +
                                "YIELD rel RETURN count(rel)",
                        Map.of("edges", edgeMaps));

                return null;
            });
        } catch (Exception e) {
            // 抛出运行时异常，以便 @Transactional 能回滚MySQL的事务
            throw new RuntimeException("Failed to save graph to Neo4j. " + e.getMessage(), e);
        }
    }

    public Map<String, List<Map<String, Object>>> getGraphComponents(long graphId) {
        try (Session session = neo4jDriver.session()) {

            // 1. 获取所有节点
            List<Map<String, Object>> nodes = session.readTransaction(tx -> {
                Result result = tx.run(
                        // 查找所有 graphId 匹配的节点
                        // (!!!) [lbl IN labels(n) WHERE lbl <> 'KnowledgeNode'][0]
                        // 这行Cypher代码用于提取您的特定标签 (KnowledgeModule/Unit/Point)
                        "MATCH (n:KnowledgeNode {graphId: $graphId}) "
                                +
                                "RETURN "
                                +
                                "  n.nodeId AS nodeId, "
                                +
                                "  n.name AS name, "
                                +
                                "  n.description AS description, "
                                +
                                "  [lbl IN labels(n) WHERE lbl <> 'KnowledgeNode'][0] AS label"
                        , Map.of("graphId", graphId)
                );
                return result.list(r -> r.asMap());
            });

            if (nodes.isEmpty()) {
                // 如果在Neo4j中也找不到（例如数据不同步），也抛出404
                throw new ResourceNotFoundException("Graph components not found in Neo4j for id: "
                        + graphId);
            }

            // 2. 获取所有边
            // 2. 获取所有边
            List<Map<String, Object>> edges = session.readTransaction(tx -> {
                Result result = tx.run(
                        // 查找该图谱中，节点之间的所有关系
                        "MATCH (source:KnowledgeNode {graphId: $graphId})-[r]->(target:KnowledgeNode {graphId: $graphId}) "
                                +
                                // (!!!) [修正] 添加 'contains' 到 WHERE 条件中
                                "WHERE type(r) = 'HAS_CHILD' OR type(r) = 'RELATED_TO' OR type(r) = 'contains' "
                                +
                                "RETURN "
                                +
                                "  coalesce(r.edgeId, toString(id(r))) AS edgeId, "
                                +
                                "  source.nodeId AS sourceNodeId, "
                                +
                                "  target.nodeId AS targetNodeId, "
                                +
                                // (!!!) 你的 coalesce 逻辑现在可以正确工作了
                                // 它会把 HAS_CHILD 和 contains (假设它们没有 r.relationType 属性) 都映射为 "contains"
                                "  coalesce(r.relationType, 'contains') AS relationType"
                        , Map.of("graphId", graphId)
                );
                return result.list(r -> r.asMap());
            });
            return Map.of("nodes", nodes, "edges", edges);
        }
    }


    /**
     * 在 Neo4j 中创建单个节点，并可选地将其连接到父节点
     *
     *
     @param
     graphId 图谱ID
     *
     @param
     request 包含新节点信息和可选 parentId 的请求
     *
     @return
     包含新节点信息的 Map (nodeId, name, description, label)
     */
    public Map<String, Object> createNodeAndLink(long graphId, CreateNodeRequest request)
    {

        // 1. 为新节点生成一个唯一的 nodeId
        String newNodeId = "node-" + UUID.randomUUID().toString();
        Map<String, Object> params = new HashMap<>();
        params.put("graphId", graphId);
        params.put("newNodeId", newNodeId);
        params.put("name", request.getName());
        params.put("description", request.getDescription());
        params.put("label", request.getLabel()); // "KnowledgeUnit" or "KnowledgePoint"
        params.put("parentId", request.getParentId()); // 可以为 null

        String relationType = "contains";

        // (!!!) 这里我们使用 StringBuilder 来构建查询，更高效
        StringBuilder cypherBuilder = new StringBuilder();

        // 2. Cypher: 创建节点
        cypherBuilder.append("CALL apoc.create.node([$label, 'KnowledgeNode'], ")
                .append("{nodeId: $newNodeId, name: $name, description: $description, graphId: $graphId}) ")
                .append("YIELD node ");

        // 3. Cypher: (可选) 如果提供了 parentId，则创建关系
        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            String newEdgeId = "edge-" + UUID.randomUUID().toString();
            params.put("newEdgeId", newEdgeId); // 放入参数 map
            cypherBuilder.append("WITH node ")
                    .append("MATCH (parent:KnowledgeNode {nodeId: $parentId, graphId: $graphId}) ")
                    .append("CALL apoc.create.relationship(parent, $relationType, {edgeId: $newEdgeId}, node) ") // <--- 修改这里
                    .append("YIELD rel ");
            params.put("relationType", relationType);
        }

        // 4. Cypher: 返回新创建的节点数据
        cypherBuilder.append("RETURN ")
                .append("  node.nodeId AS nodeId, ")
                .append("  node.name AS name, ")
                .append("  node.description AS description, ")
                .append("  [lbl IN labels(node) WHERE lbl <> 'KnowledgeNode'][0] AS label");

        // (!!!)
        // (!!!) 解决报错的关键 (!!!)
        // (!!!)
        // 1. 在 lambda 外部，将最终构建好的查询字符串赋给一个 final 变量
        final String cypherQuery = cypherBuilder.toString();

        try (Session session = neo4jDriver.session()) {
            // 5. 执行事务
            return session.writeTransaction(tx -> {
                        // 2. 在 lambda 内部，使用这个 final 变量
                Result result = tx.run(cypherQuery, params);
                if (result.hasNext()) {
                            return result.single().asMap();
                } else {
                            // 如果 MATCH (parent...) 失败 (例如 parentId 不存在)，
                    throw new ResourceNotFoundException("Failed to create node. Parent node with id '"
                            + request.getParentId() + "' not found in graph " + graphId);
                }
            });
        }
        catch (Exception e) {
            // 捕获所有Neo4j异常并重新抛出
            throw new RuntimeException("Error in createNodeAndLink: " + e.getMessage(), e);
        }
    }



    // (!!!)
// (!!!) 这是 updateNodeProperties 方法的【最终修正版 V2】
// (!!!)
    /**
     * 在单个事务中更新节点属性，并检查名称是否重复
     * @param graphId 图谱ID
     *...
     */
    public void updateNodeProperties(long graphId, String nodeId, UpdateNodeRequest request) {

        // (!!!)
        // (!!!) 这是【修正版】的 Cypher 查询 (!!!)
        // (!!!)
        // 错误 V1: YIELD from void
        // 错误 V2: Variable 'n' already declared
        // 错误 V3: Expression in WITH must be aliased
        //
        // 最终修正:
        // 1. 移除了所有 WITH 子句中的 $name 和 $description。
        // 2. $name, $description 作为参数，在所有地方 (OPTIONAL MATCH, CALL, SET) 都可用。

        final String query =
                // 1. 找到节点 'n'
                "MATCH (n:KnowledgeNode {nodeId: $nodeId, graphId: $graphId}) " +
                        "WITH n " + // <-- (!!!) 修正: 只传递 'n'

                        // 2. 找到潜在的重复 'existing'
                        "OPTIONAL MATCH (existing:KnowledgeNode {name: $name, graphId: $graphId}) " +
                        "WHERE n <> existing " +
                        "WITH n, existing " + // <-- (!!!) 修正: 只传递 'n' 和 'existing'

                        // 3. 子查询 *只* 用于验证
                        // ($name 在这里是可用的, 因为它是参数)
                        "CALL { " +
                        "    WITH n, existing " + // <-- (!!!) 修正: 只传递 'n' 和 'existing'
                        "    CALL apoc.util.validate( " +
                        "      ($name IS NOT NULL AND existing IS NOT NULL), " +
                        "      '保存失败 节点[%s]名称重复', [$name] " +
                        "    ) " +
                        "} " +

                        // 4. SET 操作在主查询中
                        // ($name 和 $description 在这里也是可用的)
                        "SET " +
                        "  n.name = coalesce($name, n.name), " +
                        "  n.description = coalesce($description, n.description) " +

                        // 5. 返回更新后的节点
                        "RETURN n";

        // (!!!) 2. 准备参数 (这部分不变)
        Map<String, Object> params = new HashMap<>();
        params.put("graphId", graphId);
        params.put("nodeId", nodeId);
        params.put("name", request.getName());
        params.put("description", request.getDescription());

        try (Session session = neo4jDriver.session()) {

            // (!!!) 3. 执行事务 (这部分不变)
            session.writeTransaction(tx -> {
                Result result = tx.run(query, params);

                if (!result.hasNext()) {
                    throw new ResourceNotFoundException("Node not found with id: " + nodeId + " in graph: " + graphId);
                }
                return null;
            });

        } catch (org.neo4j.driver.exceptions.ClientException e) {

            // (!!!) 4. 捕获异常 (这部分不变)
            // (现在, 只有 *真正* 的 "名称重复" 错误才会触发这个 if)
            if (e.getMessage() != null && e.getMessage().contains("名称重复")) {
                throw new CustomGraphException(409,e.getMessage());
            } else {
                // (所有语法错误, 比如 '...must be aliased',
                // 将从这里被抛出, 导致一个 500 错误,
                // 这才是正确的行为, 因为它表明代码本身有bug)
                throw new RuntimeException("Neo4j update failed: " + e.getMessage(), e);
            }
        }
    }


    // 在 GraphRepository.java 中添加

    /**
     * 删除节点
     * 注意：使用 DETACH DELETE 会同时删除与该节点相连的所有边
     *
     * @return true 表示删除成功，false 表示节点不存在
     * @throws CustomGraphException 如果节点有子节点，禁止删除
     */
    public boolean deleteNode(long graphId, String nodeId) {
        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {
                // 1. 检查是否有子节点 (出于安全考虑，通常不允许直接删除中间节点)
                // 我们查找是否有从该节点出发的 'contains' 关系
                String checkChildrenQuery = "MATCH (n:KnowledgeNode {nodeId: $nodeId, graphId: $graphId}) " +
                        "OPTIONAL MATCH (n)-[r:contains]->(child) " +
                        "RETURN count(child) AS childCount";

                Result checkResult = tx.run(checkChildrenQuery, Map.of("graphId", graphId, "nodeId", nodeId));

                if (checkResult.hasNext()) {
                    long childCount = checkResult.single().get("childCount").asLong();
                    if (childCount > 0) {
                        // (!!!) 抛出 30213 错误，防止图谱断裂
                        throw new CustomGraphException(400, "删除失败：该节点包含下级子节点，请先删除子节点");
                    }
                }

                // 2. 执行删除 (DETACH DELETE 会自动删除关联的边，防止Neo4j报错)
                String deleteQuery = "MATCH (n:KnowledgeNode {nodeId: $nodeId, graphId: $graphId}) " +
                        "DETACH DELETE n " +
                        "RETURN count(n) as deletedCount";

                Result deleteResult = tx.run(deleteQuery, Map.of("graphId", graphId, "nodeId", nodeId));

                if (deleteResult.hasNext()) {
                    return deleteResult.single().get("deletedCount").asLong() > 0;
                }
                return false;
            });
        }
    }

    // 在 GraphRepository 类中添加

    /**
     * 创建边
     * @return 新边的属性 Map
     */
    public Map<String, Object> createEdge(long graphId, String sourceNodeId, String targetNodeId, String relationType) {

        // 生成唯一的边 ID
        String newEdgeId = "edge-" + UUID.randomUUID().toString();

        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {

                // 1. 检查两个节点是否存在
                // 我们尝试一次性匹配两个节点
                String checkQuery = "MATCH (s:KnowledgeNode {nodeId: $sourceId, graphId: $graphId}) " +
                        "MATCH (t:KnowledgeNode {nodeId: $targetId, graphId: $graphId}) " +
                        "RETURN s, t";

                Result checkResult = tx.run(checkQuery, Map.of(
                        "graphId", graphId,
                        "sourceId", sourceNodeId,
                        "targetId", targetNodeId
                ));

                if (!checkResult.hasNext()) {
                    throw new ResourceNotFoundException("Source or Target node not found in graph " + graphId);
                }

                // 2. 创建关系 (使用 APOC 或 标准 Cypher 均可，这里用标准 Cypher 更直观，配合动态类型用 APOC)
                // 使用 APOC 是为了能够动态传入 relationType 字符串
                String createQuery =
                        "MATCH (s:KnowledgeNode {nodeId: $sourceId, graphId: $graphId}) " +
                                "MATCH (t:KnowledgeNode {nodeId: $targetId, graphId: $graphId}) " +
                                "CALL apoc.create.relationship(s, $relType, {edgeId: $edgeId}, t) YIELD rel " +
                                "RETURN " +
                                "  $edgeId AS edgeId, " +
                                "  s.nodeId AS sourceNodeId, " +
                                "  t.nodeId AS targetNodeId, " +
                                "  type(rel) AS relationType";

                Result result = tx.run(createQuery, Map.of(
                        "graphId", graphId,
                        "sourceId", sourceNodeId,
                        "targetId", targetNodeId,
                        "edgeId", newEdgeId,
                        "relType", relationType
                ));

                if (result.hasNext()) {
                    return result.single().asMap();
                }
                throw new RuntimeException("Failed to create edge.");
            });
        }
    }


    // 在 GraphRepository 类中添加

    /**
     * 删除指定边
     * @return true if deleted, false if not found
     */
    public boolean deleteEdge(long graphId, String edgeId) {
        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {
                // 匹配特定 graphId 下的特定 edgeId 的关系
                // 注意：我们在 MATCH 中包含 graphId 是为了确保只能删除当前图谱内的边
                String query = "MATCH (s:KnowledgeNode {graphId: $graphId})-[r {edgeId: $edgeId}]->(t:KnowledgeNode {graphId: $graphId}) " +
                        "DELETE r " +
                        "RETURN count(r) as deletedCount";

                Result result = tx.run(query, Map.of(
                        "graphId", graphId,
                        "edgeId", edgeId
                ));

                if (result.hasNext()) {
                    return result.single().get("deletedCount").asLong() > 0;
                }
                return false;
            });
        }
    }


    /**
     * 更新边 (包括重连节点或修改类型)
     * 实现逻辑:
     * 1. 查旧值
     * 2. 合并新旧值
     * 3. 校验自环 (30214)
     * 4. 删除旧边 -> 创建新边 (保持 edgeId 不变)
     */
    public Map<String, Object> updateEdge(long graphId, String edgeId, String newSourceId, String newTargetId, String newType) {

        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {

                // 1. 先查找旧边，获取它当前连接的节点和类型
                String findQuery =
                        "MATCH (s:KnowledgeNode {graphId: $graphId})-[r {edgeId: $edgeId}]->(t:KnowledgeNode {graphId: $graphId}) " +
                                "RETURN s.nodeId AS oldSourceId, t.nodeId AS oldTargetId, type(r) AS oldType";

                Result findResult = tx.run(findQuery, Map.of("graphId", graphId, "edgeId", edgeId));

                if (!findResult.hasNext()) {
                    return null; // 边不存在
                }

                // 获取记录 (确保 import org.neo4j.driver.Record;)
                Record record = findResult.single();
                String oldSourceId = record.get("oldSourceId").asString();
                String oldTargetId = record.get("oldTargetId").asString();
                String oldType = record.get("oldType").asString();

                // 2. 确定最终要使用的值 (如果参数为null或空，则沿用旧值)
                // 这样能确保即使只传了 targetId，finalSourceId 也是有值的(旧值)
                String finalSourceId = (newSourceId != null && !newSourceId.isBlank()) ? newSourceId : oldSourceId;
                String finalTargetId = (newTargetId != null && !newTargetId.isBlank()) ? newTargetId : oldTargetId;
                String finalType = (newType != null && !newType.isBlank()) ? newType : oldType;

                // 3. [关键] 严格校验自环
                // 此时 finalSourceId 和 finalTargetId 都是肯定有值的，可以直接比较
                if (finalSourceId.equals(finalTargetId)) {
                    throw new CustomGraphException(400, "不能创建自环关系(源节点和目标节点相同)");
                }

                // 4. 检查是否有变化，如果没有变化，直接返回旧数据，节省一次写操作
                if (finalSourceId.equals(oldSourceId) &&
                        finalTargetId.equals(oldTargetId) &&
                        finalType.equals(oldType)) {
                    return Map.of(
                            "edgeId", edgeId,
                            "sourceNodeId", finalSourceId,
                            "targetNodeId", finalTargetId,
                            "relationType", finalType
                    );
                }

                // 5. 执行更新 (先删旧边，再建新边)
                String updateQuery =
                        // Step A: 删除旧边
                        "MATCH (oldS:KnowledgeNode {graphId: $graphId})-[r {edgeId: $edgeId}]->(oldT:KnowledgeNode {graphId: $graphId}) " +
                                "DELETE r " +
                                "WITH oldS " + // WITH 子句用于分隔查询部分

                                // Step B: 查找新的起始和目标节点
                                "MATCH (newS:KnowledgeNode {nodeId: $finalSourceId, graphId: $graphId}) " +
                                "MATCH (newT:KnowledgeNode {nodeId: $finalTargetId, graphId: $graphId}) " +

                                // Step C: 使用 APOC 创建新类型的关系，并把 ID 赋给它
                                "CALL apoc.create.relationship(newS, $finalType, {edgeId: $edgeId}, newT) YIELD rel " +

                                "RETURN " +
                                "  $edgeId AS edgeId, " +
                                "  newS.nodeId AS sourceNodeId, " +
                                "  newT.nodeId AS targetNodeId, " +
                                "  type(rel) AS relationType";

                Result updateResult = tx.run(updateQuery, Map.of(
                        "graphId", graphId,
                        "edgeId", edgeId,
                        "finalSourceId", finalSourceId,
                        "finalTargetId", finalTargetId,
                        "finalType", finalType
                ));

                if (updateResult.hasNext()) {
                    return updateResult.single().asMap();
                } else {
                    // 如果代码走到这里，说明 findResult 有值(旧边存在)，但 updateResult 没值
                    // 这通常意味着 MATCH newS 或 MATCH newT 失败了 (新指定的节点ID不存在)
                    throw new ResourceNotFoundException("更新失败：指定的新源节点或目标节点在图谱中不存在");
                }
            });
        }
    }
}