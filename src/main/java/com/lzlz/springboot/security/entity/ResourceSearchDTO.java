package com.lzlz.springboot.security.entity;

import lombok.Data;

/**
 * 资源检索入参DTO
 */
@Data
public class ResourceSearchDTO {
    /**
     * 文件标题（模糊匹配）
     */
    private String fileName;

    /**
     * 上传人ID（可选筛选）
     */
    private Long uploaderId;

    /**
     * 文件类型（可选筛选，如PDF、DOCX）
     */
    private String fileType;

    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10）
     */
    private Integer pageSize = 10;
}
