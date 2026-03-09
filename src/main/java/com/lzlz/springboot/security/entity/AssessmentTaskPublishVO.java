package com.lzlz.springboot.security.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自评任务发布请求VO（接收教师提交的任务+条目）
 */
@Data
public class AssessmentTaskPublishVO {
    private Long courseId; // 必传，课程ID
    private String taskTitle; // 必传，任务标题
    private String taskDesc; // 可选，任务描述
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime; // 可选，截止时间
    private List<AssessmentItemVO> itemList; // 必传，自评条目列表
}
