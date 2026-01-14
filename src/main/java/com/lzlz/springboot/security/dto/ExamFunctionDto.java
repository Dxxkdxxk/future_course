package com.lzlz.springboot.security.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExamFunctionDto {

    /**
     * 1. 发布测试请求参数
     */
    @Data
    public static class PublishRequest {
        private Long classId;             // 发给哪个班
        private String title;             // 测试标题
        private List<String> questionIds; // 勾选的题目ID列表
        private LocalDateTime startTime;  // 开始时间
        private LocalDateTime deadline;   // 截止时间
        private Integer duration;         // 时长(分钟)
        private Integer passScore;        // 及格分(可选)
    }


    /**
     * [新增] 教师端-测试任务摘要
     */
    @Data
    public static class TaskSummary
    {
        private
        Long taskId;
        private
        Long paperId;
        private
        String title;
        private
        LocalDateTime startTime;
        private
        LocalDateTime deadline;

        // 状态 (0:未开始, 1:进行中, 2:已结束)
        // 建议在Service层根据当前时间动态计算返回，或者直接读库
        private
        Integer status;

        // 统计数据
        private Integer submittedCount;    // 已交人数
        private Integer totalStudentCount; // 班级总人数
    }


    /**
     * [新增] 学生端-我的测试任务视图
     */
    @Data
    public static class StudentTaskView
    {
        private
        Long taskId;
        private
        Long paperId;
        private
        String title;
        private
        LocalDateTime startTime;
        private
        LocalDateTime deadline;
        private Integer duration;      // 考试时长(分钟)

        // 任务本身的状态 (基于时间)
        // 0: 未开始 (时间未到), 1: 进行中, 2: 已截止
        private
        Integer taskStatus;

        // 学生个人的状态 (基于记录)
        // 0: 未参加 (可开始考试), 1: 已提交(待批改), 2: 已完成(出成绩)
        private
        Integer myStatus;

        private Integer myScore;       // 我的得分 (仅当 myStatus=2 时显示)
    }


    /**
     * [新增] 学生端-试卷内容视图 (不含答案)
     */
    @Data
    public static class PaperView
    {
        private
        Long taskId;
        private
        Long paperId;
        private String title;          // 试卷/测试标题
        private Integer duration;      // 考试时长(分钟)
        private LocalDateTime deadline;// 截止时间
        private Integer remainingSeconds; // 剩余秒数 (可选，用于前端倒计时)

        private List<QuestionItem> questions; // 题目列表
    }

    /**
     * [新增] 单个题目视图 (脱敏版)
     */
    @Data
    public static class QuestionItem
    {
        private String id;             // 题目ID
        private String type;           // 单选题/多选题/填空题/简答题
        private String stem;           // 题干 (包含选项内容)
        private Integer score;         // 分值
        private Integer sortOrder;     // 题号
    }


    // ... 在 ExamFunctionDto 类中添加 ...

    /**
     * [新增] 提交试卷请求
     */
    @Data
    public static class SubmitRequest {
        // Key: QuestionId (题目ID), Value: User Answer (学生答案)
        // 注意：多选题用逗号隔开(A,B)，填空题用分号隔开(Answer1;Answer2)
        private Map<String, String> answers;
    }

    /**
     * [新增] 提交结果响应
     */
    @Data
    public static class SubmitResult {
        private Integer finalScore; // 如果全客观题，直接出分；否则为null
        private String message;     // 提示信息
    }


    // ... 在 ExamFunctionDto 类中 ...

    /**
     * [新增] 学生提交情况列表项
     */
    @Data
    public static class StudentSubmissionDto {
        private Long recordId;        // 提交记录ID (未考则为null)
        private Integer studentId;
        private String studentName;
        private String studentNo;     // 学号/用户名

        // 状态: 0:未开始, 1:进行中, 2:待批改(已交), 3:已完成(出分)
        // 注意：这里为了前端展示方便，我们重新定义了一套状态码，或者沿用数据库状态
        // 建议：
        // -1: 未参加 (无记录)
        //  0: 答题中 (有记录但status=0)
        //  1: 待批改 (status=1)
        //  2: 已完成 (status=2)
        private Integer status;

        private Integer totalScore;   // 成绩 (未出分为null)
        private LocalDateTime submitTime; // 提交时间 (取 record.updatedAt 或 createdAt)
    }

    // ... 在 ExamFunctionDto 类中 ...

    /**
     * [新增] 阅卷视图 (教师端)
     */
    @Data
    public static class GradingView {
        private Long recordId;
        private Integer studentId;
        private String studentName;
        private Integer totalScore;    // 当前总分
        private Integer fullScore;     // 卷面满分 (可选)
        private List<GradingQuestionItem> questions;
    }

    /**
     * [新增] 阅卷题目明细
     */
    @Data
    public static class GradingQuestionItem {
        private String id;             // 题目ID
        private String stem;           // 题干
        private String type;           // 题型
        private Integer score;         // 本题满分
        private Integer sortOrder;     // 题号

        private String studentAnswer;  // 学生填写的答案
        private String standardAnswer; // 标准答案
        private Integer gainedScore;   // 学生实际得分
        private Boolean isCorrect;     // 是否自动判定为正确

        // 辅助字段：是否为主观题 (用于前端高亮需要人工批改的题)
        private Boolean isSubjective;
    }


// ... 在 ExamFunctionDto 类中 ...

    /**
     * [新增] 教师评分请求
     */
    @Data
    public static class GradeRequest {
        // 支持批量提交多道题的分数
        private List<QuestionGrade> grades;

        @Data
        public static class QuestionGrade {
            private String questionId; // 题目ID
            private Integer score;     // 教师给出的分数
        }
    }


    // ... 在 ExamFunctionDto 类中 ...

    /**
     * [新增] 学生考后结果视图
     */
    @Data
    public static class StudentResultView {
        private Long taskId;
        private String title;
        private Integer totalScore;    // 学生总分
        private Integer fullScore;     // 试卷满分
        private List<ResultQuestionItem> questions;
    }

    /**
     * [新增] 结果题目明细
     */
    @Data
    public static class ResultQuestionItem {
        private String id;             // 题目ID
        private String stem;           // 题干
        private String type;           // 题型
        private Integer score;         // 本题满分
        private Integer sortOrder;     // 题号

        private String studentAnswer;  // 学生写的
        private String standardAnswer; // 标准答案
        private String analysis;       // 解析 (核心价值)

        private Integer gainedScore;   // 得分
        private Boolean isCorrect;     // 是否正确
    }
}