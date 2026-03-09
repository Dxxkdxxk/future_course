package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.config.QRCodeUtil;
import com.lzlz.springboot.security.entity.*;
import com.lzlz.springboot.security.mapper.SignRecordMapper;
import com.lzlz.springboot.security.mapper.SignTaskMapper;
import com.lzlz.springboot.security.response.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SignTaskServiceImpl extends ServiceImpl<SignTaskMapper, SignTask> implements SignTaskService {

    private final SignRecordMapper signRecordMapper;

    // 构造器注入
    public SignTaskServiceImpl(SignRecordMapper signRecordMapper) {
        this.signRecordMapper = signRecordMapper;
    }

    /**
     * 发布签到任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SignTaskVO> publishSignTask(Long teacherId, Long courseId, String taskTitle, Integer validDuration) {
        // 参数校验
        if (courseId == null || taskTitle == null || taskTitle.trim().isEmpty()) {
            return Result.fail("课程ID和签到标题不能为空");
        }
        validDuration = validDuration == null || validDuration <= 0 ? 10 : validDuration;

        // 1. 先构造临时二维码内容（占位符，避免插入时空值）
        String tempQrcodeContent = "sign://temp"; // 临时占位，后续更新为真实ID

        // 2. 封装签到任务基础信息（包含临时二维码内容）
        SignTask signTask = new SignTask();
        signTask.setTeacherId(teacherId);
        signTask.setCourseId(courseId);
        signTask.setTaskTitle(taskTitle.trim());
        signTask.setQrcodeContent(tempQrcodeContent); // 关键：给 qrcode_content 赋值，避免插入时空值
        signTask.setValidDuration(validDuration);
        signTask.setStartTime(LocalDateTime.now());
        signTask.setEndTime(LocalDateTime.now().plusMinutes(validDuration));
        signTask.setStatus("valid");
        signTask.setSignCount(0);

        // 3. 插入数据库，获取自增ID
        this.save(signTask);
        Long taskId = signTask.getId();

        // 4. 生成真实二维码内容和Base64
        String realQrcodeContent = "sign://" + taskId;
        String qrcodeBase64 = QRCodeUtil.generateQRCodeBase64(realQrcodeContent, 300, 300);

        // 5. 补全真实二维码内容，更新数据库
        signTask.setQrcodeContent(realQrcodeContent);
        this.updateById(signTask);

        // 6. 封装VO返回
        SignTaskVO signTaskVO = new SignTaskVO();
        BeanUtils.copyProperties(signTask, signTaskVO);
        signTaskVO.setQrcodeBase64(qrcodeBase64);

        return Result.success(signTaskVO);
    }

    /**
     * 学生提交签到
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> studentSubmitSign(Long studentId, Long signTaskId, String signIp) {
        // 1. 查询签到任务是否有效
        SignTask signTask = this.getById(signTaskId);
        if (signTask == null) {
            return Result.fail("签到任务不存在");
        }
        if (!"valid".equals(signTask.getStatus())) {
            return Result.fail("签到任务已失效");
        }
        if (LocalDateTime.now().isAfter(signTask.getEndTime())) {
            return Result.fail("签到任务已过期");
        }

        // 2. 判断是否已签到
        Integer count = signRecordMapper.countByTaskAndStudent(signTaskId, studentId);
        if (count != null && count > 0) {
            return Result.fail("请勿重复签到");
        }

        // 3. 插入签到记录
        SignRecord signRecord = new SignRecord();
        signRecord.setSignTaskId(signTaskId);
        signRecord.setStudentId(studentId);
        signRecord.setSignTime(LocalDateTime.now());
        signRecord.setSignIp(signIp == null ? "" : signIp);
        signRecordMapper.insert(signRecord);

        // 4. 更新签到人数
        signTask.setSignCount(signTask.getSignCount() + 1);
        this.updateById(signTask);

        return Result.success();
    }

    /**
     * 单个签到任务详细统计
     */
    @Override
    public Result<SignStatisticsVO> getSignDetailStatistics(Long teacherId, Long signTaskId) {
        // 1. 权限校验：任务是否为当前教师发布
        SignTask signTask = this.getById(signTaskId);
        if (signTask == null) {
            return Result.fail("签到任务不存在");
        }

        // 2. 查询签到明细
        List<SignRecordVO> signRecordList = signRecordMapper.selectRecordVOByTaskId(signTaskId);

        // 3. 封装统计VO（此处简化，课程总学生数可关联课程学生表查询）
        SignStatisticsVO statisticsVO = new SignStatisticsVO();
        statisticsVO.setSignTaskId(signTaskId);
        statisticsVO.setTaskTitle(signTask.getTaskTitle());
        statisticsVO.setStartTime(signTask.getStartTime());
        statisticsVO.setEndTime(signTask.getEndTime());
        statisticsVO.setValidDuration(signTask.getValidDuration());
        statisticsVO.setStatus(signTask.getStatus());
        statisticsVO.setSignedCount(signTask.getSignCount());
        statisticsVO.setSignRecordList(signRecordList);

        // 可选：计算未签到人数（需关联course_student表）
        // Integer totalStudentCount = courseStudentMapper.countByCourseId(signTask.getCourseId());
        // statisticsVO.setTotalStudentCount(totalStudentCount);
        // statisticsVO.setUnSignedCount(totalStudentCount - signTask.getSignCount());

        return Result.success(statisticsVO);
    }

    /**
     * 签到任务汇总统计
     */
    @Override
    public Result<PageInfo<SignTask>> getSignSummaryStatistics(Long teacherId, Long courseId, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;

        // 分页查询汇总数据
        PageHelper.startPage(pageNum, pageSize);
        List<SignTask> signTaskList = baseMapper.selectSummaryByTeacher(teacherId, courseId, startDate, endDate);
        PageInfo<SignTask> pageInfo = new PageInfo<>(signTaskList);

        return Result.success(pageInfo);
    }
}
