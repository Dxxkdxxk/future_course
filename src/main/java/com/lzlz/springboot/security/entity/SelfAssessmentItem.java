package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("self_assessment_item")
public class SelfAssessmentItem {
    /** 条目主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联自评任务ID */
    private Long taskId;
    /** 自评条目内容（如："Spring Boot 核心注解"） */
    private String itemContent;
    /** 条目排序号（用于前端展示顺序） */
    private Integer sortNum;
}
