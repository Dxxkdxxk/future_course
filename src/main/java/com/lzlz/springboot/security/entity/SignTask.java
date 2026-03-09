package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sign_task")
public class SignTask {
    /** 签到任务ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 发布教师ID */
    private Long teacherId;
    /** 课程ID */
    private Long courseId;
    /** 签到任务标题 */
    private String taskTitle;
    /** 二维码内容（sign://{id}） */
    private String qrcodeContent;
    /** 有效时长（分钟） */
    private Integer validDuration;
    /** 发布时间 */
    private LocalDateTime startTime;
    /** 截止时间 */
    private LocalDateTime endTime;
    /** 任务状态（valid：有效，invalid：过期，cancel：已取消） */
    private String status;
    /** 已签到人数 */
    private Integer signCount;
}
