package com.lzlz.springboot.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeworkDetailResponse {
    private Long id;
    private String title;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    private Integer status; // 0:草稿 1:已发布

    // 题目列表
    private List<QuestionDetailItem> questions;

    @Data
    public static class QuestionDetailItem {
        private String questionId;
        private String stem;       // 题干
        private String type;       // 题型 (单选/多选/简答等)
        private Integer score;     // 本次作业中设定的分值
        private Integer sortOrder; // 排序
        // 教师端可能还需要看参考答案，以便预览
        private String answer;
        private String analysis;
    }
}