package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeEvaluationResponse {
    private Long courseId;
    private Long graphId;
    private String evaluationTime;
    private Double overallMasteryLevel;
    private String aiAnalysis;
    private List<KnowledgePointItem> knowledgePoints;

    @Data
    public static class KnowledgePointItem {
        private String knowledgePointId;
        private String knowledgePointName;
        private String label;
        private Boolean isImportant;
        private Double masteryScore;
        private LearningDataItem learningData;
        private Boolean isWeak;
    }

    @Data
    public static class LearningDataItem {
        private Long totalDuration;
        private Double resourceProgress;
        private Double exerciseScoreRate;
        private Double testScoreRate;
    }
}
