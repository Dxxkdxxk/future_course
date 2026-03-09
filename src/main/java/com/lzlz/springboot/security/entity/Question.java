package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("questions")
public class Question {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("course_id")
    private Long courseId;

    private String stem;           // 题干
    private String type;           // 类型
    private String topic;          // 知识点
    private String difficulty;     // 难度
    private Integer score;         // 分值

    @TableField("estimated_time")
    private Integer estimatedTime; // 预估时间

    // (!!!) 新增字段
    private String answer;         // 标准答案
    private String analysis;       // 解析

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}