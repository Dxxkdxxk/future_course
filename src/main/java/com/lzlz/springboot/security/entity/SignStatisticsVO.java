package com.lzlz.springboot.security.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 签到统计详细VO
 */
@Data
public class SignStatisticsVO {
    // 任务基本信息
    private Long signTaskId;
    private String taskTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer validDuration;
    private String status;
    // 统计数据
    private Integer totalStudentCount; // 课程总学生数（可选）
    private Integer signedCount; // 已签到人数
    private Integer unSignedCount; // 未签到人数（可选）
    // 签到明细
    private List<SignRecordVO> signRecordList;
}
