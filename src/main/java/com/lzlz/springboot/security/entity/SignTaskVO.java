package com.lzlz.springboot.security.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 签到任务响应VO（含二维码Base64）
 */
@Data
public class SignTaskVO {
    private Long id;
    private Long courseId;
    private String taskTitle;
    private String qrcodeBase64; // 二维码Base64编码
    private Integer validDuration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer signCount;
}



