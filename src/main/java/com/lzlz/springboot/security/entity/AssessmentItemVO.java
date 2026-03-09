package com.lzlz.springboot.security.entity;

import lombok.Data;

/**
 * 自评条目VO
 */
@Data
public class AssessmentItemVO {
    private String itemContent; // 必传，条目内容
    private Integer sortNum; // 可选，排序号（默认按提交顺序）
}

