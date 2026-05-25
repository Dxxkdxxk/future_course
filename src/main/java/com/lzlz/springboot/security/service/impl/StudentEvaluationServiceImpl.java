package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.GraphNodeProgress;
import com.lzlz.springboot.security.dto.AbilityEvaluationResponse;
import com.lzlz.springboot.security.dto.GraphKnowledgeNode;
import com.lzlz.springboot.security.dto.KnowledgeEvaluationResponse;
import com.lzlz.springboot.security.dto.NodeProgressSnapshot;
import com.lzlz.springboot.security.dto.RadarChartResponse;
import com.lzlz.springboot.security.entity.AbilityPoint;
import com.lzlz.springboot.security.entity.GraphMetadata;
import com.lzlz.springboot.security.entity.KnowledgeAbilityMapping;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.AbilityPointMapper;
import com.lzlz.springboot.security.mapper.GraphMetadataMapper;
import com.lzlz.springboot.security.mapper.KnowledgeAbilityMappingMapper;
import com.lzlz.springboot.security.repository.GraphRepository;
import com.lzlz.springboot.security.service.GraphLearningProgressService;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import com.lzlz.springboot.security.service.StudentEvaluationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StudentEvaluationServiceImpl implements StudentEvaluationService {
    private static final double WEAK_THRESHOLD = 60.0d;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StudentCourseAccessService studentCourseAccessService;
    private final GraphMetadataMapper graphMetadataMapper;
    private final GraphRepository graphRepository;
    private final GraphLearningProgressService graphLearningProgressService;
    private final AbilityPointMapper abilityPointMapper;
    private final KnowledgeAbilityMappingMapper knowledgeAbilityMappingMapper;

    public StudentEvaluationServiceImpl(StudentCourseAccessService studentCourseAccessService,
                                        GraphMetadataMapper graphMetadataMapper,
                                        GraphRepository graphRepository,
                                        GraphLearningProgressService graphLearningProgressService,
                                        AbilityPointMapper abilityPointMapper,
                                        KnowledgeAbilityMappingMapper knowledgeAbilityMappingMapper) {
        this.studentCourseAccessService = studentCourseAccessService;
        this.graphMetadataMapper = graphMetadataMapper;
        this.graphRepository = graphRepository;
        this.graphLearningProgressService = graphLearningProgressService;
        this.abilityPointMapper = abilityPointMapper;
        this.knowledgeAbilityMappingMapper = knowledgeAbilityMappingMapper;
    }

    @Override
    public KnowledgeEvaluationResponse getKnowledgeEvaluation(Integer studentId, Long courseId, Long graphId, Boolean isLatest) {
        long resolvedGraphId = resolveAndCheckGraph(studentId, courseId, graphId);
        List<GraphKnowledgeNode> leafNodes = graphRepository.listLeafKnowledgeNodes(resolvedGraphId);
        if (!Boolean.TRUE.equals(isLatest)) {
            refreshLeafProgress(courseId, resolvedGraphId, studentId, leafNodes);
        }

        List<KnowledgeEvaluationResponse.KnowledgePointItem> items = new ArrayList<>();
        double totalMastery = 0.0d;
        for (GraphKnowledgeNode node : leafNodes) {
            NodeProgressSnapshot progress = graphRepository.getNodeProgressSnapshot(resolvedGraphId, node.getNodeId(), courseId, studentId);
            double mastery = round1(progress.getOverallProgress());
            totalMastery += mastery;

            KnowledgeEvaluationResponse.LearningDataItem learningData = new KnowledgeEvaluationResponse.LearningDataItem();
            learningData.setTotalDuration(0L);
            learningData.setResourceProgress(round1(progress.getVideoProgress()));
            learningData.setExerciseScoreRate(round1(progress.getHomeworkProgress()));
            learningData.setTestScoreRate(round1(progress.getExamProgress()));

            KnowledgeEvaluationResponse.KnowledgePointItem item = new KnowledgeEvaluationResponse.KnowledgePointItem();
            item.setKnowledgePointId(node.getNodeId());
            item.setKnowledgePointName(node.getName());
            item.setLabel(node.getLabel());
            item.setIsImportant(false);
            item.setMasteryScore(mastery);
            item.setLearningData(learningData);
            item.setIsWeak(mastery < WEAK_THRESHOLD);
            items.add(item);
        }

        KnowledgeEvaluationResponse response = new KnowledgeEvaluationResponse();
        response.setCourseId(courseId);
        response.setGraphId(resolvedGraphId);
        response.setEvaluationTime(nowText());
        response.setOverallMasteryLevel(items.isEmpty() ? 0.0d : round1(totalMastery / items.size()));
        response.setAiAnalysis("当前版本基于知识图谱节点进度生成评价，AI 分析可在此基础上接入大模型服务。");
        response.setKnowledgePoints(items);
        return response;
    }

    @Override
    public AbilityEvaluationResponse getAbilityEvaluation(Integer studentId, Long courseId, Long graphId, Boolean isLatest) {
        long resolvedGraphId = resolveAndCheckGraph(studentId, courseId, graphId);
        if (!Boolean.TRUE.equals(isLatest)) {
            refreshLeafProgress(courseId, resolvedGraphId, studentId, graphRepository.listLeafKnowledgeNodes(resolvedGraphId));
        }

        List<AbilityPoint> leafAbilities = abilityPointMapper.selectList(new QueryWrapper<AbilityPoint>()
                .eq("course_id", courseId)
                .eq("is_leaf", 1));

        List<AbilityEvaluationResponse.AbilityPointItem> items = new ArrayList<>();
        double totalScore = 0.0d;
        for (AbilityPoint ability : leafAbilities) {
            double score = calcAbilityScore(studentId, courseId, resolvedGraphId, ability.getId());
            totalScore += score;

            AbilityEvaluationResponse.AbilityPointItem item = new AbilityEvaluationResponse.AbilityPointItem();
            item.setAbilityPointId(ability.getId());
            item.setAbilityPointName(ability.getAbilityName());
            item.setAbilityScore(score);
            item.setLevel(toLevel(score));
            items.add(item);
        }

        AbilityEvaluationResponse response = new AbilityEvaluationResponse();
        response.setCourseId(courseId);
        response.setGraphId(resolvedGraphId);
        response.setOverallScore(items.isEmpty() ? 0.0d : round1(totalScore / items.size()));
        response.setAiAnalysis("当前版本基于 ability_points 与 knowledge_ability_mappings 加权生成能力评价，AI 分析可在此基础上接入大模型服务。");
        response.setAbilityPoints(items);
        return response;
    }

    @Override
    public RadarChartResponse getRadarChart(Integer studentId, Long courseId, Long graphId, String chartType, Boolean refresh) {
        String normalized = chartType == null ? "KNOWLEDGE" : chartType.trim().toUpperCase(Locale.ROOT);
        long resolvedGraphId = resolveAndCheckGraph(studentId, courseId, graphId);
        if (Boolean.TRUE.equals(refresh)) {
            refreshLeafProgress(courseId, resolvedGraphId, studentId, graphRepository.listLeafKnowledgeNodes(resolvedGraphId));
        }
        if ("ABILITY".equals(normalized)) {
            return buildAbilityRadar(studentId, courseId, resolvedGraphId);
        }
        return buildKnowledgeRadar(studentId, courseId, resolvedGraphId);
    }

    private RadarChartResponse buildKnowledgeRadar(Integer studentId, Long courseId, Long graphId) {
        List<GraphKnowledgeNode> nodes = graphRepository.listKnowledgeNodesWithParent(graphId);
        Map<String, List<GraphKnowledgeNode>> childrenMap = nodes.stream()
                .collect(Collectors.groupingBy(node -> parentKey(node.getParentNodeId())));
        Map<String, Double> scoreMap = new HashMap<>();
        for (GraphKnowledgeNode node : nodes) {
            NodeProgressSnapshot progress = graphRepository.getNodeProgressSnapshot(graphId, node.getNodeId(), courseId, studentId);
            scoreMap.put(node.getNodeId(), round1(progress.getOverallProgress()));
        }

        List<RadarChartResponse.DimensionItem> dimensions = childrenMap.getOrDefault("0", List.of()).stream()
                .map(node -> buildKnowledgeDimension(node, childrenMap, scoreMap))
                .toList();

        RadarChartResponse response = new RadarChartResponse();
        response.setChartType("KNOWLEDGE");
        response.setCourseId(courseId);
        response.setGraphId(graphId);
        response.setSnapshotTime(nowText());
        response.setDimensions(dimensions);
        return response;
    }

    private RadarChartResponse.DimensionItem buildKnowledgeDimension(GraphKnowledgeNode node,
                                                                     Map<String, List<GraphKnowledgeNode>> childrenMap,
                                                                     Map<String, Double> scoreMap) {
        List<GraphKnowledgeNode> children = childrenMap.getOrDefault(node.getNodeId(), List.of());
        RadarChartResponse.DimensionItem item = new RadarChartResponse.DimensionItem();
        item.setDimensionId(node.getNodeId());
        item.setDimensionName(node.getName());
        item.setMaxValue(100.0d);
        if (children.isEmpty()) {
            item.setDimensionValue(scoreMap.getOrDefault(node.getNodeId(), 0.0d));
            item.setHasChildren(false);
            item.setChildren(null);
            return item;
        }
        List<RadarChartResponse.DimensionItem> childItems = children.stream()
                .map(child -> buildKnowledgeDimension(child, childrenMap, scoreMap))
                .toList();
        item.setDimensionValue(round1(childItems.stream()
                .map(RadarChartResponse.DimensionItem::getDimensionValue)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0d)));
        item.setHasChildren(true);
        item.setChildren(childItems);
        return item;
    }

    private RadarChartResponse buildAbilityRadar(Integer studentId, Long courseId, Long graphId) {
        List<AbilityPoint> abilities = abilityPointMapper.selectList(new QueryWrapper<AbilityPoint>()
                .eq("course_id", courseId));
        Map<Long, List<AbilityPoint>> childrenMap = abilities.stream()
                .collect(Collectors.groupingBy(ability -> ability.getParentId() == null ? 0L : ability.getParentId()));
        Map<Long, Double> scoreMap = new HashMap<>();
        for (AbilityPoint ability : abilities) {
            if (ability.getIsLeaf() != null && ability.getIsLeaf() == 1) {
                scoreMap.put(ability.getId(), calcAbilityScore(studentId, courseId, graphId, ability.getId()));
            }
        }

        List<RadarChartResponse.DimensionItem> dimensions = childrenMap.getOrDefault(0L, List.of()).stream()
                .map(ability -> buildAbilityDimension(ability, childrenMap, scoreMap))
                .toList();

        RadarChartResponse response = new RadarChartResponse();
        response.setChartType("ABILITY");
        response.setCourseId(courseId);
        response.setGraphId(graphId);
        response.setSnapshotTime(nowText());
        response.setDimensions(dimensions);
        return response;
    }

    private RadarChartResponse.DimensionItem buildAbilityDimension(AbilityPoint ability,
                                                                   Map<Long, List<AbilityPoint>> childrenMap,
                                                                   Map<Long, Double> scoreMap) {
        List<AbilityPoint> children = childrenMap.getOrDefault(ability.getId(), List.of());
        RadarChartResponse.DimensionItem item = new RadarChartResponse.DimensionItem();
        item.setDimensionId(String.valueOf(ability.getId()));
        item.setDimensionName(ability.getAbilityName());
        item.setMaxValue(100.0d);
        if (children.isEmpty()) {
            item.setDimensionValue(scoreMap.getOrDefault(ability.getId(), 0.0d));
            item.setHasChildren(false);
            item.setChildren(null);
            return item;
        }
        List<RadarChartResponse.DimensionItem> childItems = children.stream()
                .map(child -> buildAbilityDimension(child, childrenMap, scoreMap))
                .toList();
        item.setDimensionValue(round1(childItems.stream()
                .map(RadarChartResponse.DimensionItem::getDimensionValue)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0d)));
        item.setHasChildren(true);
        item.setChildren(childItems);
        return item;
    }

    private double calcAbilityScore(Integer studentId, Long courseId, Long graphId, Long abilityPointId) {
        List<KnowledgeAbilityMapping> mappings = knowledgeAbilityMappingMapper.selectList(
                new QueryWrapper<KnowledgeAbilityMapping>()
                        .eq("graph_id", graphId)
                        .eq("ability_point_id", abilityPointId));
        if (mappings.isEmpty()) {
            return 0.0d;
        }
        double weightedSum = 0.0d;
        double totalWeight = 0.0d;
        for (KnowledgeAbilityMapping mapping : mappings) {
            double weight = mapping.getContributionWeight() == null ? 1.0d : mapping.getContributionWeight();
            NodeProgressSnapshot progress = graphRepository.getNodeProgressSnapshot(graphId, mapping.getNodeId(), courseId, studentId);
            weightedSum += progress.getOverallProgress() * weight;
            totalWeight += weight;
        }
        return totalWeight <= 0.0d ? 0.0d : round1(weightedSum / totalWeight);
    }

    private void refreshLeafProgress(Long courseId, Long graphId, Integer studentId, List<GraphKnowledgeNode> leafNodes) {
        for (GraphKnowledgeNode node : leafNodes) {
            GraphNodeProgress ignored = graphLearningProgressService.recalculateNodeProgress(courseId, graphId, node.getNodeId(), studentId);
        }
    }

    private long resolveAndCheckGraph(Integer studentId, Long courseId, Long graphId) {
        if (courseId == null) {
            throw new CustomGraphException(400, "courseId is required");
        }
        studentCourseAccessService.checkCourseAccess(studentId, courseId);
        if (graphId != null) {
            GraphMetadata graph = graphMetadataMapper.selectById(graphId);
            if (graph == null) {
                throw new ResourceNotFoundException("Graph not found with id: " + graphId);
            }
            if (!Objects.equals(graph.getCourseId(), courseId.longValue())) {
                throw new CustomGraphException(400, "Graph does not belong to current course");
            }
            return graphId;
        }
        GraphMetadata graph = graphMetadataMapper.selectOne(new QueryWrapper<GraphMetadata>()
                .eq("course_id", courseId)
                .orderByDesc("created_at")
                .last("LIMIT 1"));
        if (graph == null) {
            throw new ResourceNotFoundException("No knowledge graph found for course: " + courseId);
        }
        return graph.getGraphId();
    }

    private String toLevel(double score) {
        if (score >= 85) {
            return "EXCELLENT";
        }
        if (score >= 70) {
            return "GOOD";
        }
        if (score >= 55) {
            return "AVERAGE";
        }
        return "POOR";
    }

    private String parentKey(String parentNodeId) {
        return parentNodeId == null || parentNodeId.isBlank() ? "0" : parentNodeId;
    }

    private String nowText() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private double round1(double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }
}
