package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGradeSubmissionRequest {
    /**
     * 教师补充给 AI 的批改要求，可为空
     */
    private String extraInstruction;

    /**
     * 得分点描述，仅作为 AI 分项评价依据，不单独赋分
     */
    private List<ScoringPoint> scoringPoints;

    @Data
    public static class ScoringPoint {
        private String description;
    }
}
