package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paper_questions")
public class PaperQuestion {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("paper_id")
    private Long paperId;

    @TableField("question_id")
    private String questionId;

    @TableField("sort_order")
    private Integer sortOrder;

    private Integer score; // 这里预留了覆盖分数字段，暂且不用
}