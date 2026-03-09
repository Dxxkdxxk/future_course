package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("homework_questions")
public class HomeworkQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long homeworkId;
    private String questionId; // 关联 Question 表的 UUID
    private Integer score;     // 题目在本次作业中的分值
    private Integer sortOrder; // 排序
}