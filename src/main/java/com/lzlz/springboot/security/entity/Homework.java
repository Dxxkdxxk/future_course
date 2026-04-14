package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("homeworks")
public class Homework {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private String title;

    @TableField("content")
    private String content;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("allow_late_submit")
    private Integer allowLateSubmit;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("attachment_urls")
    private String attachmentUrls;

    // 0: draft, 1: published
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
