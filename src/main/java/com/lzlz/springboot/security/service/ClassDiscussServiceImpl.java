package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.entity.ClassDiscuss;
import com.lzlz.springboot.security.mapper.ClassDiscussMapper;
import com.lzlz.springboot.security.response.Result;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
public class ClassDiscussServiceImpl extends ServiceImpl<ClassDiscussMapper, ClassDiscuss> implements ClassDiscussService {

    @Override
    public Result<Void> publishDiscuss(Long userId, String userType, Long courseId, String content) {
        // 参数校验
        if (courseId == null || content == null || content.trim().isEmpty()) {
            return Result.fail("课程ID和评论内容不能为空");
        }
        if (content.length() > 1000) {
            return Result.fail("评论内容不能超过1000字");
        }

        // 封装实体
        ClassDiscuss discuss = new ClassDiscuss();
        discuss.setCourseId(courseId);
        discuss.setUserId(userId);
        discuss.setUserType(userType);
        discuss.setContent(content.trim());
        discuss.setCreateTime(LocalDateTime.now());

        // 插入数据库
        boolean save = this.save(discuss);
        return save ? Result.success() : Result.fail("评论发布失败");
    }

    @Override
    public Result<PageInfo<ClassDiscuss>> getDiscussList(Long courseId, Integer pageNum, Integer pageSize) {
        // 参数默认值
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;

        // 分页查询
        PageHelper.startPage(pageNum, pageSize);
        PageInfo<ClassDiscuss> pageInfo = new PageInfo<>(
                this.lambdaQuery()
                        .eq(ClassDiscuss::getCourseId, courseId)
                        .orderByDesc(ClassDiscuss::getCreateTime)
                        .list()
        );

        return Result.success(pageInfo);
    }
}
