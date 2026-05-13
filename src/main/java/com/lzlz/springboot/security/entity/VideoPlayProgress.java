package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_play_progress")
public class VideoPlayProgress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;          // 用户ID
    private Long courseId;        // 课程ID
    private String videoId;       // 视频ID（resourceId）
    private Integer progressSeconds; // 已观看秒数
    private Double progressPercentage; // 【关键】进度百分比（加上这行！）
    private LocalDateTime updateTime; // 更新时间
}
