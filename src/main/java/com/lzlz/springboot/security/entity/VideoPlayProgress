package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_play_progress")
public class VideoPlayProgress {
    private Long id;
    private Long userId;
    private String videoId;  // 改成 String
    private Integer progressSeconds;
    private LocalDateTime updateTime;
}
