package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("papers")
public class Paper {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("course_id")
    private Long courseId;

    private String title;
    private String description;
    private String difficulty;
    private Integer duration;

    @TableField("total_score")
    private Integer totalScore;

    // (!!!) 新增字段：及格分
    @TableField("pass_score")
    private Integer passScore;

    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}