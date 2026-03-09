package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class CourseUpdateDto {

    // (!!!) 1. 添加 'name' 字段，以便Service层可以更新它
    private String name;

    // 2. 保留您原有的所有字段
    private String background;
    private String position;
    private String goal;
    private String feature;
    private String knowledgeLogic;
    private String teachingPlan;

    // (!!!) 3. 'description' 字段已被删除
}