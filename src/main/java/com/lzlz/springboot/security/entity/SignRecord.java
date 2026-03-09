package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sign_record")
public class SignRecord {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联签到任务ID */
    private Long signTaskId;
    /** 学生ID */
    private Long studentId;
    /** 签到时间 */
    private LocalDateTime signTime;
    /** 签到IP */
    private String signIp;
}
