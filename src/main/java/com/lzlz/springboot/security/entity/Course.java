package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("courses")
public class Course {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("class_id")
    private Long classId;

    @TableField("course_no")
    private String courseNo;

    private String term;

    @TableField("course_intro")
    private String courseIntro;

    @TableField("course_outline")
    private String courseOutline;

    private String goal;

    private String feature;

    @TableField("course_textbook")
    private String courseTextbook;

    @TableField("teaching_plan_object_name")
    private String teachingPlanObjectName;

    @TableField("teaching_plan_name")
    private String teachingPlanName;

    @TableField(exist = false)
    private String teachingPlanUrl;

    @TableField("course_team")
    private String courseTeam;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
