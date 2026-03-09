package com.lzlz.springboot.security.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssessmentTaskDetailVO {
    private Long id;
    private Long courseId;
    private String taskTitle;
    private String taskDesc;
    private LocalDateTime publishTime;
    private LocalDateTime endTime;
    private String status;
    private List<StudentAssessmentItemVO> itemList;
}
