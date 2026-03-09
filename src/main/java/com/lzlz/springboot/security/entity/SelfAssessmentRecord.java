package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("self_assessment_record")
public class SelfAssessmentRecord {
    /** 记录主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联自评任务ID */
    private Long taskId;
    /** 关联自评条目ID */
    private Long itemId;
    /** 学生ID */
    private Long studentId;
    /** 掌握程度（A：熟练掌握 B：较好掌握 C：基本掌握 D：掌握较差 E：没掌握） */
    private String masterLevel;
    /** 提交时间 */
    private LocalDateTime submitTime;
}
