package com.lzlz.springboot.security.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资源统计返回DTO
 */
@Data
public class ResourceStatisticDTO {
    /**
     * 分组维度值（如uploaderId、courseId）
     */
    private Long groupId;

    /**
     * 该分组下资源总数
     */
    private Integer totalCount;

    /**
     * 按文件类型分类统计（如PDF:5, DOC:3）
     */
    private List<FileTypeCountDTO> typeCountList;

    /**
     * 该分组下所有资源详情
     */
    private List<ResourceDetailDTO> resourceDetailList;

    // 子DTO：文件类型统计
    @Data
    public static class FileTypeCountDTO {
        private String fileType; // 文件类型（如PDF、DOCX）
        private Integer count;   // 该类型数量
    }

    // 子DTO：资源详情
    @Data
    public static class ResourceDetailDTO {
        private Long id;                 // 资源ID
        private String name;             // 文件名
        private String fileType;         // 文件类型
        private String fileSize;         // 格式化后的文件大小（如1.5MB）
        private Long fileSizeBytes;      // 原始文件大小（字节）
        private LocalDateTime createTime;// 上传时间
        private Long uploaderId;         // 上传人ID
        private Long courseId;           // 关联课程ID（按课程统计时返回）
        private Long chapterId;          // 关联章节ID（按课程统计时返回）
        private String minioObjectName;
        private String presignedUrl;
    }
}
