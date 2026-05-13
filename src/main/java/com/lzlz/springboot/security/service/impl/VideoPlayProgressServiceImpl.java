package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lzlz.springboot.security.dto.SaveVideoProgressDTO;
import com.lzlz.springboot.security.entity.VideoPlayProgress;
import com.lzlz.springboot.security.entity.VideoProgressVO;
import com.lzlz.springboot.security.mapper.VideoPlayProgressMapper;
import com.lzlz.springboot.security.service.VideoPlayProgressService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoPlayProgressServiceImpl implements VideoPlayProgressService {

    @Resource
    private VideoPlayProgressMapper progressMapper;

     @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateProgress(Long userId, Long courseId, SaveVideoProgressDTO dto) {
        // 1. 三条件唯一查询
        LambdaQueryWrapper<VideoPlayProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoPlayProgress::getUserId, userId)
               .eq(VideoPlayProgress::getCourseId, courseId)
               .eq(VideoPlayProgress::getVideoId, dto.getResourceId());

        VideoPlayProgress progress = progressMapper.selectOne(wrapper);

        // 2. 【核心】自动计算进度百分比
        Double percentage = calculateProgressPercentage(
                dto.getWatchedSeconds(),
                dto.getDurationSeconds()
        );

        if (progress == null) {
            // 新增：存储秒数 + 百分比
            progress = new VideoPlayProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setVideoId(dto.getResourceId());
            progress.setProgressSeconds(dto.getWatchedSeconds());
            progress.setProgressPercentage(percentage); // 存百分比
            progressMapper.insert(progress);
        } else {
            // 更新：秒数 + 百分比一起更新
            progress.setProgressSeconds(dto.getWatchedSeconds());
            progress.setProgressPercentage(percentage); // 更新百分比
            progressMapper.updateById(progress);
        }
    }

        private Double calculateProgressPercentage(Integer watchedSeconds, Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return 0.00; // 总时长异常，返回0
        }
        // 计算：(已看 / 总时长) * 100，保留2位小数
        double percent = (double) watchedSeconds / durationSeconds * 100;
        return Math.round(percent * 100) / 100.0;
    }
@Override
public VideoProgressVO getProgress(Long userId, Long courseId, String resourceId) {
    LambdaQueryWrapper<VideoPlayProgress> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(VideoPlayProgress::getUserId, userId)
           .eq(VideoPlayProgress::getCourseId, courseId)
           .eq(VideoPlayProgress::getVideoId, resourceId);

    VideoPlayProgress progress = progressMapper.selectOne(wrapper);
    
    if (progress == null) {
        return new VideoProgressVO(0, 0.00);
    }
    // 返回秒数 + 百分比
    return new VideoProgressVO(progress.getProgressSeconds(), progress.getProgressPercentage());
}
}
