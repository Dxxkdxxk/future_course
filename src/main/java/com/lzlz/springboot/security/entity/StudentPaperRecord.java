package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_paper_records") // 对应数据库表名
public class StudentPaperRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private int studentId;

    @TableField("paper_id")
    private Long paperId;

    @TableField("chapter_id")
    private Long chapterId;

    @TableField("total_score")
    private Integer totalScore; // 学生得分

    @TableField("full_score")
    private Integer fullScore;  // 卷面满分

    @TableField("is_passed")
    private Boolean isPassed;   // 是否及格

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}