package com.lzlz.springboot.security.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoProgressVO {
    private Integer watchedSeconds; // 已观看秒数
    private Double progressPercentage; // 进度百分比
}
