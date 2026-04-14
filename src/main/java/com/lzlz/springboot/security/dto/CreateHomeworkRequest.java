package com.lzlz.springboot.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class CreateHomeworkRequest {
    private String title;
    private String content;

    // Backward compatible field; if content is empty, service falls back to description.
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Boolean allowLateSubmit;
    private Integer totalScore;

    // Comma-separated MinIO object names.
    private String attachmentUrls;
}
