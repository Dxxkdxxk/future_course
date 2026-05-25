package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGradeSubmissionResponse {
    private Integer score;
    private String comment;
    private List<ScoringPointResult> scoringPointResults;

    @Data
    public static class ScoringPointResult {
        private String description;
        private String completion;
        private String comment;
    }
}
