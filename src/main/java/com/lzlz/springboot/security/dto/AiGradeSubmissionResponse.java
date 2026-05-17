package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGradeSubmissionResponse {
    private Integer score;
    private String comment;
    private String summary;
    private List<String> problems;
    private List<String> suggestions;
    private String basis;
    private String rawResponse;
}
