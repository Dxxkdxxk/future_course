package com.lzlz.springboot.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateHomeworkRequest {
    private String title;
    private String description;

    // 接收前端的时间格式 "yyyy-MM-dd HH:mm:ss"
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    // 题目列表
    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {
        private String questionId;
        private Integer score;
    }
}