package com.lzlz.springboot.security.dto;

import lombok.Data;
import java.util.List;

public class ExamDto {

    // ================== 教师端请求 ==================

    /** 1. 创建试卷请求 */
    @Data
    public static class CreatePaperRequest {
        private String title;          // 试卷标题
        private String description;    // 描述
        private Integer duration;      // 考试时长(分钟)
        private List<String> questionIds; // 从题库中挑选的题目ID列表
    }

    /** 2. 发布试卷请求 (绑定章节) */
    @Data
    public static class PublishRequest {
        private Long paperId;
    }

    // ================== 学生端请求/响应 ==================

    /** 3. 学生提交答案请求 */
    @Data
    public static class SubmitRequest {
        private List<AnswerItem> answers;
    }

    @Data
    public static class AnswerItem {
        private String questionId;
        private String myAnswer;
    }

    /** 4. 试卷视图 (用于学生考试，无答案) */
    @Data
    public static class PaperView {
        private Long paperId;
        private String title;
        private Integer duration;
        private Integer totalScore;
        private List<QuestionView> questions;
    }

    @Data
    public static class QuestionView {
        private String id;
        private String stem;
        private String type;      // single, multiple, etc.
        private String difficulty;
        private Integer score;    // 该题在试卷中的分值
        // (!!!) 绝对不返回 answer 和 analysis
    }

    /** 5. 考试结果响应 */
    @Data
    public static class ResultView {
        private Long recordId;
        private Integer myScore;
        private Integer totalScore;
        private Boolean isPassed;
        private Integer correctCount;
        // 可选：返回详情列表让学生看解析
    }
}