package com.lzlz.springboot.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentHomeworkDetailDto {
    private Long id;
    private String title;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Boolean allowLateSubmit;
    private Integer totalScore;

    private boolean submitted;
    private Integer status; // 0: not submitted, 1: submitted, 2: graded

    // Computed by server when now > endTime
    private Boolean lateWindow;

    private List<String> homeworkAttachmentUrls;
    private List<String> attachmentUrls;

    private Integer finalScore;
    private String teacherComment;
}
