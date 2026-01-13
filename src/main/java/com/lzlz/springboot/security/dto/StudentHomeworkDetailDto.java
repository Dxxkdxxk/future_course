package com.lzlz.springboot.security.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentHomeworkDetailDto {
    // --- 1. 作业元数据 ---
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;

    // --- 2. 题目列表 ---
    private List<StudentQuestionItem> questions;

    // --- 3. 学生提交状态 ---
    private boolean submitted;      // 是否已提交
    private Integer status;         // 状态 (0:未交, 1:已交, 2:已批改)

    // [删除] private String content;  <-- 不需要了，因为没有作答文本

    private List<String> attachmentUrls; // 学生只通过文件提交

    // --- 4. 批改结果 ---
    private Integer finalScore;
    private String teacherComment;

    /**
     * 内部类：题目详情
     */
    @Data
    public static class StudentQuestionItem {
        private String questionId;
        private String stem;       // 题干 (选项内容包含在这里面)
        private String type;

        // [删除] private String options; <-- 不需要了，选项在题干里

        private Integer score;
        private Integer sortOrder;
    }
}