package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzlz.springboot.security.entity.SelfAssessmentItem;
import com.lzlz.springboot.security.entity.SelfAssessmentRecord;
import com.lzlz.springboot.security.entity.SelfAssessmentTask;
import com.lzlz.springboot.security.entity.StudentAssessmentSubmitVO;
import com.lzlz.springboot.security.mapper.SelfAssessmentItemMapper;
import com.lzlz.springboot.security.mapper.SelfAssessmentRecordMapper;
import com.lzlz.springboot.security.mapper.SelfAssessmentTaskMapper;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.response.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SelfAssessmentStudentServiceImpl extends ServiceImpl<SelfAssessmentRecordMapper, SelfAssessmentRecord> implements SelfAssessmentStudentService {

    private final SelfAssessmentTaskMapper selfAssessmentTaskMapper;
    private final SelfAssessmentRecordMapper selfAssessmentRecordMapper;

    private final SelfAssessmentItemMapper selfAssessmentItemMapper;
    // 构造器注入
    public SelfAssessmentStudentServiceImpl(SelfAssessmentItemMapper selfAssessmentItemMapper, SelfAssessmentTaskMapper selfAssessmentTaskMapper, SelfAssessmentRecordMapper selfAssessmentRecordMapper) {
        this.selfAssessmentTaskMapper = selfAssessmentTaskMapper;
        this.selfAssessmentRecordMapper = selfAssessmentRecordMapper;
        this.selfAssessmentItemMapper = selfAssessmentItemMapper;

    }

    /**
     * 学生提交自评结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> submitAssessment(Long studentId, StudentAssessmentSubmitVO submitVO) {
        // 1. 参数校验
        Long taskId = submitVO.getTaskId();
        if (taskId == null || CollectionUtils.isEmpty(submitVO.getItemResultList())) {
            return Result.fail("任务ID和自评结果不能为空");
        }

        // 2. 校验任务是否有效
        SelfAssessmentTask task = selfAssessmentTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.fail("自评任务不存在");
        }
        if (!"valid".equals(task.getStatus())) {
            return Result.fail("该自评任务已失效");
        }
        if (task.getEndTime() != null && LocalDateTime.now().isAfter(task.getEndTime())) {
            return Result.fail("该自评任务已过期，无法提交");
        }

        // 3. 校验是否已提交
        Integer count = selfAssessmentRecordMapper.countByTaskAndStudent(taskId, studentId);
        if (count != null && count > 0) {
            return Result.fail("请勿重复提交自评");
        }

        // 4. 批量插入自评记录
        for (var itemResult : submitVO.getItemResultList()) {
            // 校验掌握程度格式
            String masterLevel = itemResult.getMasterLevel();
            if (!List.of("A", "B", "C", "D", "E").contains(masterLevel)) {
                return Result.fail("掌握程度格式错误，仅支持A/B/C/D/E");
            }

            SelfAssessmentRecord record = new SelfAssessmentRecord();
            record.setTaskId(taskId);
            record.setItemId(itemResult.getItemId());
            record.setStudentId(studentId);
            record.setMasterLevel(masterLevel);
            record.setSubmitTime(LocalDateTime.now());
            selfAssessmentRecordMapper.insert(record);
        }

        return Result.success();
    }

    @Override
    public Result<List<SelfAssessmentItem>> getTaskDetail(Long taskId) {
        List<SelfAssessmentItem> selfAssessmentItems = selfAssessmentItemMapper.selectByTaskId(taskId);
        return Result.success(selfAssessmentItems);
    }

    @Override
    public Result<PageInfo<SelfAssessmentTask>> getValidTaskList(Long studentId, Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;

        PageHelper.startPage(pageNum, pageSize);
        List<SelfAssessmentTask> taskList = selfAssessmentTaskMapper.selectValidTaskForStudent(studentId);
        PageInfo<SelfAssessmentTask> pageInfo = new PageInfo<>(taskList);

        return Result.success(pageInfo);
    }
}
