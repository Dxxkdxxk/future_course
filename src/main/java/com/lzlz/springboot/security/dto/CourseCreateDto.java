package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class CourseCreateDto {
    private String name;
    private Long teacherId;
    private Long classId;
    private String courseNo;
    private String term;
    private String courseIntro;
    private String courseOutline;
    private String goal;
    private String feature;
    private String courseTextbook;
    private String teachingPlanObjectName;
    private String teachingPlanName;
    private String courseTeam;
}
