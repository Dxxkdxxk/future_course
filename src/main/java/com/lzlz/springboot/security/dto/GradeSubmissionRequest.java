package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class GradeSubmissionRequest {
    /**
     * 最终得分 (0-100)
     */
    private Integer score;

    /**
     * 教师评语
     */
    private String comment;
}