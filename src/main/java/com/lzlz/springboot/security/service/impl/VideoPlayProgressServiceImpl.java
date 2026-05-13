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
        // 【核心】三字段联合查询：用户 + 课程 + 视频
        LambdaQueryWrapper<VideoPlayProgress wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoPlayProgress::getUserId, userId)
               .eq(VideoPlayProgress::getCourseId, courseId)  // 新增
               .eq(VideoPlayProgress::getVideoId, dto.getResourceId());

        VideoPlayProgress progress = progressMapper.selectOne(wrapper);
        
        if (progress == null) {
            // 新增：存储 courseId
            progress = new VideoPlayProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId); // 新增
            progress.setVideoId(dto.getResourceId());
            progress.setProgressSeconds(dto.getWatchedSeconds());
            progressMapper.insert(progress);
        } else {
            // 更新进度
            progress.setProgressSeconds(dto.getWatchedSeconds());
            progressMapper.updateById(progress);
        }
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
