package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.SaveVideoProgressDTO;

public interface VideoPlayProgressService {
    // 保存/更新：增加 courseId
    void saveOrUpdateProgress(Long userId, Long courseId, SaveVideoProgressDTO dto);

    // 查询：增加 courseId
    VideoProgressVO getProgress(Long userId, Long courseId, String resourceId);
}
