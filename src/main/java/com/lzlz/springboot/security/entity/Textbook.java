package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("textbook")
public class Textbook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String fileType;
    private String minioBucket;
    private String minioObjectName;
    private String status;
    private Long fileSize;
    private Long uploaderId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PARSING = "PARSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";
}
