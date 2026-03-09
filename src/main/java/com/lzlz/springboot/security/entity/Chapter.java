package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("chapter")
public class Chapter {

    /**
     * 章节ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联教材ID
     */
    @TableField("textbook_id")
    private Long textbookId;

    /**
     * 父章节ID（顶级章节为NULL）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 层级：1=章，2=节，3=小节
     */
    @TableField("level")
    private Integer level;

    /**
     * 章节标题
     */
    @TableField("title")
    private String title;

    /**
     * 排序号（保证章节顺序）
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 章节正文（可选）
     */
    @TableField("content")
    private String content;

    /**
     * PDF对应页码（仅PDF文件）
     */
    @TableField("pdf_page")
    private Integer pdfPage;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 子章节列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Chapter> children;
}
