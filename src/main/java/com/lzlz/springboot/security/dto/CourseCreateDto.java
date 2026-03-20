package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class CourseCreateDto {
    private String name;
    private Long teacherId;
    private Long classId;
    private String courseNo;
    private String term;
    private String background;
    private String position;
    private String goal;
    private String feature;
    private String knowledgeLogic;
    private String teachingPlan;
}
