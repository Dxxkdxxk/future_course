package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.List;

@Data
public class AbilityEvaluationResponse {
    private Long courseId;
    private Long graphId;
    private Double overallScore;
    private String aiAnalysis;
    private List<AbilityPointItem> abilityPoints;

    @Data
    public static class AbilityPointItem {
        private Long abilityPointId;
        private String abilityPointName;
        private Double abilityScore;
        private String level;
    }
}
