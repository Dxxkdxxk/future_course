package com.lzlz.springboot.security.dto;

import com.lzlz.springboot.security.entity.Question;
import lombok.Data;
import java.util.List;

public class PaperDto {

    /**
     * 创建试卷请求 (手动组卷)
     */
    @Data
    public static class CreateRequest {
        private String title;
        private String description;
        private Integer duration;      // 考试时长
        private String difficulty;     // 难度标记

        // 前端传来的题目ID列表 (老师勾选了哪些题)
        private List<String> questionIds;
    }

    /**
     * 智能组卷请求 (随机抽题)
     */
    @Data
    public static class AutoCreateRequest {
        private String title;
        private Integer duration;
        private Integer questionCount; // 只要多少题
        private String difficulty;     // 只要什么难度的
        // private String topic;       // (可选) 只要哪个知识点的
    }

    /**
     * 试卷详情响应 (包含题目列表)
     */
    @Data
    public static class DetailResponse {
        private Long id;
        private String title;
        private String description;
        private Integer duration;
        private Integer totalScore;

        // 包含完整的题目信息
        private List<Question> questions;
    }
}