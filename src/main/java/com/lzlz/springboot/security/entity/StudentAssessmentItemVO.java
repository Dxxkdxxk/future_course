package com.lzlz.springboot.security.entity;

import lombok.Data;

@Data
public class StudentAssessmentItemVO {
    private Long itemId; // 必传，条目ID
    private String masterLevel; // 必传，掌握程度（A/B/C/D/E）
    private String itemContent;
}
