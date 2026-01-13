package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对应数据库表: student_homeworks
 * 仅记录作业维度的提交文件、总分和总评
 */
@Data
@TableName("student_homeworks")
public class HomeworkSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long homeworkId;
    private int studentId;

    // 作业附件URL集合 (Mock模式下传字符串即可)
    private String attachmentUrls;

    // 状态: 0:未提交 1:已提交(待批改) 2:已批改
    private Integer status;

    // 最终得分 (整个作业的分数)
    private Integer finalScore;

    // 教师整体评语
    private String teacherComment;

    // 提交时间
    private LocalDateTime submittedAt;

    // 批改时间
    private LocalDateTime gradedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}