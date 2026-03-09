package com.lzlz.springboot.security.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资源检索返回DTO
 */
@Data
public class ResourceSearchResultDTO {
    /**
     * 总条数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 当前页数据
     */
    private List<ResourceDetailDTO> records;

    /**
     * 资源详情子DTO（包含课程/章节关联信息）
     */
    @Data
    public static class ResourceDetailDTO {
        /**
         * 资源ID
         */
        private Long id;

        /**
         * 文件名（标题）
         */
        private String name;

        /**
         * 文件类型（如PDF、DOCX）
         */
        private String fileType;

        /**
         * 格式化后的文件大小（如1.5MB）
         */
        private String fileSize;

        /**
         * 原始文件大小（字节）
         */
        private Long fileSizeBytes;

        /**
         * 上传时间
         */
        private LocalDateTime createTime;

        /**
         * 上传人ID
         */
        private Long uploaderId;

        /**
         * MinIO存储桶名
         */
        private String minioBucket;

        /**
         * MinIO对象名（文件路径）
         */
        private String minioObjectName;

        /**
         * 资源状态（PENDING/PARSING/SUCCESS/FAIL）
         */
        private String status;

        /**
         * 关联课程ID（无则为null）
         */
        private Long courseId;

        /**
         * 关联章节ID（无则为null）
         */
        private Long chapterId;
    }
}
