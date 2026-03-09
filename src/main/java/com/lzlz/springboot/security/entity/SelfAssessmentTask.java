package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("self_assessment_task")
public class SelfAssessmentTask {
    /** 自评任务主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 发布教师ID */
    private Long teacherId;
    /** 课程ID */
    private Long courseId;
    /** 自评任务标题 */
    private String taskTitle;
    /** 自评任务描述（可选） */
    private String taskDesc;
    /** 发布时间 */
    private LocalDateTime publishTime;
    /** 截止时间（可选，null表示永久有效） */
    private LocalDateTime endTime;
    /** 任务状态（valid：有效，invalid：过期，cancel：已取消） */
    private String status;
    @TableField(exist = false)
    private List<Long> studentIds;
}
