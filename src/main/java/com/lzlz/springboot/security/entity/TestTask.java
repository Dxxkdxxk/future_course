package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 测试任务表 (test_tasks)
 * 用于记录将哪张试卷(paper_id)发布给了哪个班级(class_id)
 */
@Data
@TableName("test_tasks")
public class TestTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("paper_id")
    private Long paperId;   // 关联生成的试卷

    @TableField("class_id")
    private Long classId;   // 关联目标班级

    private String title;   // 测试标题 (如: "期中摸底测试")

    @TableField("start_time")
    private LocalDateTime startTime;

    private LocalDateTime deadline; // 截止时间

    private Integer duration;      // 考试限时(分钟)

    // 状态: 0:未开始, 1:进行中, 2:已结束
    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}