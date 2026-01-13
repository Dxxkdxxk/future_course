package com.lzlz.springboot.security.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentSubmissionDto {
    // 学生信息
    private Long studentId;
    private String studentName;
    private String studentEmail;

    // 提交详情
    private Long submissionId;
    private List<String> attachmentUrls;
    private String content;       // 备注
    private Integer status;       // 1:已提交, 2:已批改

    // 批改结果
    private Integer finalScore;
    private String teacherComment;

    // 时间
    private LocalDateTime submittedAt;
    private LocalDateTime gradedAt;
}