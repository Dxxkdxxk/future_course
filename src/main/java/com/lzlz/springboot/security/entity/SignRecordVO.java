package com.lzlz.springboot.security.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签到记录VO
 */
@Data
public class SignRecordVO {
    private Long id;
    private Long signTaskId;
    private Long studentId;
    private String studentName; // 学生姓名（关联用户表）
    private LocalDateTime signTime;
    private String signIp;
}
