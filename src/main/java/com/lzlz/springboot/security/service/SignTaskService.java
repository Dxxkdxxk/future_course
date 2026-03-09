package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.entity.SignStatisticsVO;
import com.lzlz.springboot.security.entity.SignTask;
import com.lzlz.springboot.security.entity.SignTaskVO;
import com.lzlz.springboot.security.response.Result;

public interface SignTaskService extends IService<SignTask> {
    // 发布签到任务
    Result<SignTaskVO> publishSignTask(Long teacherId, Long courseId, String taskTitle, Integer validDuration);

    // 学生提交签到
    Result<Void> studentSubmitSign(Long studentId, Long signTaskId, String signIp);

    // 单个签到任务详细统计
    Result<SignStatisticsVO> getSignDetailStatistics(Long teacherId, Long signTaskId);

    // 签到任务汇总统计
    Result<PageInfo<SignTask>> getSignSummaryStatistics(Long teacherId, Long courseId, String startDate, String endDate, Integer pageNum, Integer pageSize);
}

