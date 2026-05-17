package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class AiGradeSubmissionRequest {
    /**
     * 教师补充给 AI 的批改要求，可为空
     */
    private String extraInstruction;
}
