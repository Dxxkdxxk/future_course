package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.SaveVideoProgressDTO;

public interface VideoPlayProgressService {
    // 保存进度
    void saveOrUpdateProgress(Long userId, SaveVideoProgressDTO dto);

    // 查询进度：第二个参数 改为 String resourceId（和实现类、前端对应）
    Integer getProgress(Long userId, String resourceId);
}
