package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 章节资源关联表实体类
 *
 * @since 2025-12-04
 */
@Data
@TableName("chapter_resources")
public class ChapterResources implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 课程ID
     */
    @TableField("course_id")
    private Long courseId;

    /**
     * 章节ID
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 资源ID
     */
    @TableField("resource_id")
    private Long resourceId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
