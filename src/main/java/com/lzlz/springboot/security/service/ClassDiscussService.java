package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.entity.ClassDiscuss;
import com.lzlz.springboot.security.response.Result;

public interface ClassDiscussService extends IService<ClassDiscuss> {
    // 发布评论
    Result<Void> publishDiscuss(Long userId, String userType, Long courseId, String content);

    // 查询评论列表
    Result<PageInfo<ClassDiscuss>> getDiscussList(Long courseId, Integer pageNum, Integer pageSize);
}
