package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("class_discuss")
public class ClassDiscuss {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 课程ID */
    private Long courseId;
    /** 用户ID（教师/学生） */
    private Long userId;
    /** 评论内容 */
    private String content;
    private String userType; // 如 "student"（学生）、"teacher"（老师）
    /** 评论时间 */
    private LocalDateTime createTime;
}
