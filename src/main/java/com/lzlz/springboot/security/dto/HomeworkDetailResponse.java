package com.lzlz.springboot.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeworkDetailResponse {
    private Long id;
    private String title;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Boolean allowLateSubmit;
    private Integer totalScore;
    private Integer status;
    private List<String> attachmentUrls;
}
