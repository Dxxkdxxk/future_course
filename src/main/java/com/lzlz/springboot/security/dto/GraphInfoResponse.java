package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用于返回课程下图谱列表的精简 DTO
 */
@Data
public class GraphInfoResponse {
    private long graphId;
    private String name;
    private LocalDateTime createdAt;
}