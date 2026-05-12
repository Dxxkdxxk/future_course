package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.SaveVideoProgressDTO;

public interface VideoPlayProgressService {
    // 保存/更新视频播放进度
    void saveOrUpdateProgress(Long userId, SaveVideoProgressDTO dto);

    // 查询当前用户的视频播放进度
    Integer getProgress(Long userId, Long videoId);
}
