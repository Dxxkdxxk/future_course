package com.lzlz.springboot.security.entity;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import com.lzlz.springboot.security.domain.ExtendReadingMaterialTypeEnum;
import com.lzlz.springboot.security.domain.Fastjson2JsonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "extend_reading", autoResultMap = true)
public class ExtendReading {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private String studentId;

    @TableField("textbook_id")
    private Long textbookId;

    @TableField("chapter_id")
    private Long chapterId;

    @TableField(value = "position", typeHandler = Fastjson2JsonTypeHandler.class)
    private JSONObject position;

    @TableField("material_type")
    private String materialType;

    @TableField("title")
    private String title;

    @TableField("url")
    private String url;

    @TableField("content")
    private String content;

    @TableField("minio_bucket")
    private String minioBucket;

    @TableField("minio_object_name")
    private String minioObjectName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_type")
    private String fileType;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public ExtendReadingMaterialTypeEnum getMaterialTypeEnum() {
        return ExtendReadingMaterialTypeEnum.getByCode(this.materialType);
    }
}
