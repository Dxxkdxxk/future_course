package com.lzlz.springboot.security.entity;

import lombok.Data;

import java.util.List;

@Data
public class StudentAssessmentSubmitVO {
    private Long taskId; // 必传，自评任务ID
    private List<StudentAssessmentItemVO> itemResultList; // 必传，条目评价结果列表
}
