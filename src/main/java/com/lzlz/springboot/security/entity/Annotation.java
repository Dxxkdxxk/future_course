package com.lzlz.springboot.security.entity;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import com.lzlz.springboot.security.domain.AnnotationTypeEnum;
import com.lzlz.springboot.security.domain.Fastjson2JsonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 标注实体类
 */
@Data
@TableName("annotation") // 对应数据库表名
public class Annotation {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /**
     * 教材ID（对应前端resourceId）
     */
    private Long resourceId;

    /**
     * 起始偏移量
     */
    private Integer startOffset;

    /**
     * 结束偏移量
     */
    private Integer endOffset;

    /**
     * 选中的文本
     */
    private String selectedText;

    /**
     * 高亮颜色
     */
    private String highlightColor;

    /**
     * 标注类型（highlight/note）
     */
    private String annotationType;

    /**
     * 备注
     */
    private String comment;

    /**
     * 创建时间（数据库自动生成，前端无需传）
     */
    private LocalDateTime createTime;

    private String minioObjectName;
}