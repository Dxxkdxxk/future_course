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
    private Long courseId;        // 【新增】课程ID
    private String videoId;       // 视频ID（对应前端resourceId）
    private Integer progressSeconds; // 播放进度
    private LocalDateTime updateTime;
}
