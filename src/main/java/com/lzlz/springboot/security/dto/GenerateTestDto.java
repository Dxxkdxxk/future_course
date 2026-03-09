package com.lzlz.springboot.security.dto;

import lombok.Data;
import java.util.List;

public class GenerateTestDto {

    /**
     * 请求体结构
     */
    @Data
    public static class Request {
        private String section;          // 对应 schema 中的 section
        private String testName;         // 对应 schema 中的 testName
        private Integer testDuration;    // 对应 schema 中的 testDuration
        private Integer totalScore;      // 对应 schema 中的 totalScore
        private List<SelectedQuestionItem> selectedQuestions; // 题目列表
    }

    /**
     * 内部题目结构
     */
    @Data
    public static class SelectedQuestionItem {
        private String id;
        private String stem;
        private String type;
        private String topic;
        private String difficulty;
        private Integer score;
        private Integer estimatedTime;
        private String answer;
        private String analysis;
    }
}