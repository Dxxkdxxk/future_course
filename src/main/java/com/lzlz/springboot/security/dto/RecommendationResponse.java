package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class RecommendationResponse {
    private Long recommendId;
    private String targetKnowledgeId;
    private String targetKnowledgeName;
    private String recommendType;
    private String itemId;
    private String itemTitle;
    private String itemFormat;
    private Integer difficultyLevel;
    private String recommendReason;
    private Boolean isCompleted;
}
