package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class CourseUpdateDto {

    // (!!!) 1. 添加 'name' 字段，以便Service层可以更新它
    private String name;
    private String courseNo;
    private String term;

    // 2. 保留您原有的所有字段
    private String courseIntro;
    private String courseOutline;
    private String goal;
    private String feature;
    private String courseTextbook;
    private String teachingPlanObjectName;
    private String teachingPlanName;
    private String courseTeam;

    // (!!!) 3. 'description' 字段已被删除
}
