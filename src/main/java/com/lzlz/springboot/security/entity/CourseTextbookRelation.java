package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_textbook_relation")
public class CourseTextbookRelation {
    private Long id;
    private Long courseId;
    private Long textbookId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
