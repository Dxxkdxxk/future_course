package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("homeworks")
public class Homework {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private String title;
    private String description;
    private LocalDateTime deadline;

    // 状态 0:未发布 1:已发布
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}