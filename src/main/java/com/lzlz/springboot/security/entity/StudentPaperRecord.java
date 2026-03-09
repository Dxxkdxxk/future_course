package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生考试记录表
 * 对应数据库表: student_paper_records
 */
@Data
@TableName("student_paper_records")
public class StudentPaperRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Integer studentId; // 注意：这里建议用Integer或Long，与User表保持一致

    @TableField("paper_id")
    private Long paperId;      // 关联的试卷ID

    @TableField("chapter_id")
    private Long chapterId;    // 关联的章节ID (如果是章节练习) 或 0 (如果是班级测试)

    @TableField("total_score")
    private Integer totalScore; // 学生当前得分 (待批改状态下可能不准或为空)

    @TableField("full_score")
    private Integer fullScore;  // 卷面满分

    @TableField("is_passed")
    private Boolean isPassed;   // 是否及格

    /**
     * [新增核心字段] 考试状态
     * 0: 答题中 (Started / In Progress)
     * 1: 已提交，待批改 (Submitted / Pending Grading) - 含主观题需老师批改
     * 2: 已完成 (Completed / Graded) - 所有题目均已批改，成绩已出
     */
    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 可选：添加更新时间，记录什么时候批改完的
    // @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    // private LocalDateTime updatedAt;
}