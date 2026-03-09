package com.lzlz.springboot.security.entity;


import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生自评记录列表 VO（适配 /record/list 接口）
 */
@Data
public class StudentRecordVO {
    // 自评记录ID
    private Long recordId;
    // 关联任务ID
    private Long taskId;
    // 任务标题
    private String taskTitle;
    // 关联条目ID
    private Long itemId;
    // 条目内容
    private String itemContent;
    // 掌握程度（A/B/C/D/E）
    private String masterLevel;
    // 提交时间
    private LocalDateTime submitTime;
}
