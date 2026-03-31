package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chapter_resource_ref")
public class ChapterResource {
    @TableId(type = IdType.AUTO)
    private Long id; // 这是数据库的主键 (1, 2, 3...)

    private Long courseId;
    private Long chapterId;

    @TableField("learning_chapter_id")
    private Long learningChapterId;

    @TableField("resource_name")
    private String resourceName;

    @TableField("material_type")
    private String materialType;

    // (!!!) 修改点：字段名变更，映射数据库新的列名 file_id
    @TableField("file_id")
    private Long fileId; // 这里存 MinIO ID

    @TableField("file_size")
    private String fileSize;

    @TableField("is_video")
    private Boolean isVideo;

    @TableField("is_required")
    private Boolean isRequired;

    private String section;
    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
