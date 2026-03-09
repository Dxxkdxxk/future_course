package com.lzlz.springboot.security.dto;

import lombok.Data;

public class QuestionDto {

    /**
     * 创建题目请求
     */
    @Data
    public static class CreateRequest {
        private String stem;
        private String type;
        private String topic;
        private String difficulty;
        private Integer score;
        private Integer estimatedTime;

        // (!!!) 新增字段
        private String answer;
        private String analysis;
    }

    /**
     * 查询参数 (保持不变)
     */
    @Data
    public static class QueryRequest {
        private String type;
        private String difficulty;
        private String keyword;
        private String questions; // ID列表字符串
    }

    /**
     * 题目响应对象
     */
    @Data
    public static class Response {
        private String id;
        private String stem;
        private String type;
        private String topic;
        private String difficulty;
        private Integer score;
        private Integer estimatedTime;

        // (!!!) 新增字段
        private String answer;
        private String analysis;
    }
}