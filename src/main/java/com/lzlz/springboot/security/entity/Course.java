package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("courses") // (!!!) 匹配您的数据库表名
public class Course {

    @TableId(value = "id", type = IdType.AUTO) // 匹配 'id' bigint AUTO_INCREMENT
    private Long id;

    private String name; // 匹配 'name'

    @TableField("teacher_id") // 匹配 'teacher_id'
    private Long teacherId;

    @TableField("class_id")
    private Long classId;

    @TableField("course_no")
    private String courseNo;

    private String term;

    private String background;
    private String position;
    private String goal;
    private String feature;

    @TableField("knowledge_logic") // 匹配 'knowledge_logic'
    private String knowledgeLogic;

    @TableField("teaching_plan") // 匹配 'teaching_plan'
    private String teachingPlan;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
