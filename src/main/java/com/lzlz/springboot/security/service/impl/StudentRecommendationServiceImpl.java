package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.GraphResourceDto;
import com.lzlz.springboot.security.dto.GraphKnowledgeNode;
import com.lzlz.springboot.security.dto.NodeProgressSnapshot;
import com.lzlz.springboot.security.dto.PageResponse;
import com.lzlz.springboot.security.dto.RecommendationResponse;
import com.lzlz.springboot.security.entity.GraphMetadata;
import com.lzlz.springboot.security.entity.Question;
import com.lzlz.springboot.security.entity.StudentPaperDetail;
import com.lzlz.springboot.security.entity.StudentPaperRecord;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.GraphMetadataMapper;
import com.lzlz.springboot.security.mapper.QuestionMapper;
import com.lzlz.springboot.security.mapper.StudentPaperDetailMapper;
import com.lzlz.springboot.security.mapper.StudentPaperRecordMapper;
import com.lzlz.springboot.security.repository.GraphRepository;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import com.lzlz.springboot.security.service.StudentRecommendationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class StudentRecommendationServiceImpl implements StudentRecommendationService {
    private static final double WEAK_THRESHOLD = 60.0d;
    private static final int MAX_RESOURCE_PER_POINT = 2;
    private static final int MAX_QUESTION_PER_POINT = 2;

    private final StudentCourseAccessService studentCourseAccessService;
    private final GraphMetadataMapper graphMetadataMapper;
    private final GraphRepository graphRepository;
    private final QuestionMapper questionMapper;
    private final StudentPaperRecordMapper studentPaperRecordMapper;
    private final StudentPaperDetailMapper studentPaperDetailMapper;

    public StudentRecommendationServiceImpl(StudentCourseAccessService studentCourseAccessService,
                                            GraphMetadataMapper graphMetadataMapper,
                                            GraphRepository graphRepository,
                                            QuestionMapper questionMapper,
                                            StudentPaperRecordMapper studentPaperRecordMapper,
                                            StudentPaperDetailMapper studentPaperDetailMapper) {
        this.studentCourseAccessService = studentCourseAccessService;
        this.graphMetadataMapper = graphMetadataMapper;
        this.graphRepository = graphRepository;
        this.questionMapper = questionMapper;
        this.studentPaperRecordMapper = studentPaperRecordMapper;
        this.studentPaperDetailMapper = studentPaperDetailMapper;
    }

    @Override
    public PageResponse<RecommendationResponse> getRecommendations(Integer studentId,
                                                                    Long courseId,
                                                                    Long graphId,
                                                                    String weakPointId,
                                                                    Boolean isCompleted,
                                                                    Integer page,
                                                                    Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        long resolvedGraphId = resolveAndCheckGraph(studentId, courseId, graphId);
        Set<String> completedQuestionIds = loadCompletedQuestionIds(studentId);

        List<WeakNode> weakNodes = graphRepository.listLeafKnowledgeNodes(resolvedGraphId).stream()
                .filter(node -> weakPointId == null || weakPointId.isBlank() || weakPointId.equals(node.getNodeId()))
                .map(node -> {
                    NodeProgressSnapshot progress = graphRepository.getNodeProgressSnapshot(
                            resolvedGraphId, node.getNodeId(), courseId, studentId);
                    return new WeakNode(node, progress.getOverallProgress() == null ? 0.0d : progress.getOverallProgress());
                })
                .filter(item -> item.masteryScore < WEAK_THRESHOLD)
                .sorted(Comparator.comparingDouble(item -> item.masteryScore))
                .toList();

        List<RecommendationResponse> recommendations = new ArrayList<>();
        long recommendId = 1L;
        for (WeakNode weakNode : weakNodes) {
            recommendId = appendResourceRecommendations(
                    recommendations, recommendId, studentId, courseId, resolvedGraphId, weakNode, isCompleted);
            recommendId = appendQuestionRecommendations(
                    recommendations, recommendId, courseId, weakNode, completedQuestionIds, isCompleted);
        }

        long total = recommendations.size();
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= total) {
            return new PageResponse<>(total, List.of());
        }
        int toIndex = Math.min(fromIndex + safePageSize, recommendations.size());
        return new PageResponse<>(total, recommendations.subList(fromIndex, toIndex));
    }

    private long appendResourceRecommendations(List<RecommendationResponse> result,
                                               long recommendId,
                                               Integer studentId,
                                               Long courseId,
                                               long graphId,
                                               WeakNode weakNode,
                                               Boolean isCompletedFilter) {
        List<GraphResourceDto.ResourceView> resources = graphRepository
                .getNodeResources(graphId, weakNode.node.getNodeId())
                .stream()
                .sorted(Comparator.comparingDouble(resource -> graphRepository.getStudentResourceProgress(
                        graphId, weakNode.node.getNodeId(), courseId, studentId, resource.getResourceId())))
                .limit(MAX_RESOURCE_PER_POINT)
                .toList();

        for (GraphResourceDto.ResourceView resource : resources) {
            double progress = graphRepository.getStudentResourceProgress(
                    graphId, weakNode.node.getNodeId(), courseId, studentId, resource.getResourceId());
            boolean completed = progress >= 1.0d;
            if (isCompletedFilter != null && !Objects.equals(isCompletedFilter, completed)) {
                continue;
            }

            RecommendationResponse response = baseResponse(recommendId++, weakNode);
            response.setRecommendType("RESOURCE");
            response.setItemId(resource.getResourceId());
            response.setItemTitle(resource.getName());
            response.setItemFormat(normalizeResourceFormat(resource));
            response.setDifficultyLevel(3);
            response.setRecommendReason(buildResourceReason(weakNode.masteryScore, response.getItemFormat()));
            response.setIsCompleted(completed);
            result.add(response);
        }
        return recommendId;
    }

    private long appendQuestionRecommendations(List<RecommendationResponse> result,
                                               long recommendId,
                                               Long courseId,
                                               WeakNode weakNode,
                                               Set<String> completedQuestionIds,
                                               Boolean isCompletedFilter) {
        List<Question> questions = queryQuestionsByNodeName(courseId, weakNode.node.getName()).stream()
                .limit(MAX_QUESTION_PER_POINT)
                .toList();

        for (Question question : questions) {
            boolean completed = completedQuestionIds.contains(question.getId());
            if (isCompletedFilter != null && !Objects.equals(isCompletedFilter, completed)) {
                continue;
            }

            RecommendationResponse response = baseResponse(recommendId++, weakNode);
            response.setRecommendType("QUESTION");
            response.setItemId(question.getId());
            response.setItemTitle(truncate(question.getStem(), 30));
            response.setItemFormat(question.getType());
            response.setDifficultyLevel(parseDifficulty(question.getDifficulty()));
            response.setRecommendReason(buildQuestionReason(weakNode.masteryScore));
            response.setIsCompleted(completed);
            result.add(response);
        }
        return recommendId;
    }

    private List<Question> queryQuestionsByNodeName(Long courseId, String nodeName) {
        if (nodeName == null || nodeName.isBlank()) {
            return List.of();
        }
        return questionMapper.selectList(new QueryWrapper<Question>()
                .eq("course_id", courseId)
                .and(wrapper -> wrapper.like("topic", nodeName).or().like("stem", nodeName))
                .last("LIMIT " + MAX_QUESTION_PER_POINT));
    }

    private Set<String> loadCompletedQuestionIds(Integer studentId) {
        List<StudentPaperRecord> records = studentPaperRecordMapper.selectList(new QueryWrapper<StudentPaperRecord>()
                .eq("student_id", studentId)
                .ge("status", 1));
        if (records == null || records.isEmpty()) {
            return Set.of();
        }
        List<Long> recordIds = records.stream()
                .map(StudentPaperRecord::getId)
                .filter(Objects::nonNull)
                .toList();
        if (recordIds.isEmpty()) {
            return Set.of();
        }
        List<StudentPaperDetail> details = studentPaperDetailMapper.selectList(new QueryWrapper<StudentPaperDetail>()
                .in("record_id", recordIds));
        Set<String> ids = new HashSet<>();
        for (StudentPaperDetail detail : details) {
            if (detail.getQuestionId() != null) {
                ids.add(detail.getQuestionId());
            }
        }
        return ids;
    }

    private RecommendationResponse baseResponse(long recommendId, WeakNode weakNode) {
        RecommendationResponse response = new RecommendationResponse();
        response.setRecommendId(recommendId);
        response.setTargetKnowledgeId(weakNode.node.getNodeId());
        response.setTargetKnowledgeName(weakNode.node.getName());
        return response;
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

    private String normalizeResourceFormat(GraphResourceDto.ResourceView resource) {
        if (resource.getMaterialType() != null && !resource.getMaterialType().isBlank()) {
            return resource.getMaterialType().trim().toUpperCase(Locale.ROOT);
        }
        if (Boolean.TRUE.equals(resource.getIsVideo())) {
            return "VIDEO";
        }
        return "RESOURCE";
    }

    private String buildResourceReason(double masteryScore, String resourceType) {
        String prefix = masteryScore < 30
                ? "该知识点掌握度较低，建议先从基础资源开始学习"
                : "该知识点存在明显薄弱，建议重点复习";
        String suffix = switch (resourceType == null ? "" : resourceType.toUpperCase(Locale.ROOT)) {
            case "VIDEO" -> "，推荐观看视频讲解加深理解";
            case "SLIDE", "PPT" -> "，推荐查看课件进行碎片化复习";
            case "DOCUMENT", "PDF" -> "，推荐阅读文档梳理知识体系";
            default -> "，推荐学习相关资料";
        };
        return prefix + suffix;
    }

    private String buildQuestionReason(double masteryScore) {
        if (masteryScore < 30) {
            return "该知识点掌握度较低，建议通过练习题检验基础理解";
        }
        return "该知识点存在薄弱，建议通过针对性练习查漏补缺";
    }

    private Integer parseDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return 3;
        }
        try {
            return Integer.parseInt(difficulty.trim());
        } catch (NumberFormatException ignored) {
            String normalized = difficulty.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("easy") || normalized.contains("简单")) {
                return 1;
            }
            if (normalized.contains("hard") || normalized.contains("困难")) {
                return 5;
            }
            return 3;
        }
    }

    private String truncate(String content, int maxLen) {
        if (content == null) {
            return "";
        }
        return content.length() > maxLen ? content.substring(0, maxLen) + "..." : content;
    }

    private record WeakNode(GraphKnowledgeNode node, double masteryScore) {
    }
}
