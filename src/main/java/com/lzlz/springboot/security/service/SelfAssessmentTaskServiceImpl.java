package com.lzlz.springboot.security.service;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzlz.springboot.security.entity.*;
import com.lzlz.springboot.security.mapper.SelfAssessmentItemMapper;
import com.lzlz.springboot.security.mapper.SelfAssessmentRecordMapper;
import com.lzlz.springboot.security.mapper.SelfAssessmentTaskMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.response.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.util.Collections;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SelfAssessmentTaskServiceImpl extends ServiceImpl<SelfAssessmentTaskMapper, SelfAssessmentTask> implements SelfAssessmentTaskService {

    private final SelfAssessmentItemMapper selfAssessmentItemMapper;

    private final SelfAssessmentTaskMapper selfAssessmentTaskMapper;

    private final SelfAssessmentRecordMapper selfAssessmentRecordMapper;
    // 构造器注入
    public SelfAssessmentTaskServiceImpl(SelfAssessmentItemMapper selfAssessmentItemMapper, SelfAssessmentTaskMapper selfAssessmentTaskMapper, SelfAssessmentRecordMapper selfAssessmentRecordMapper) {
        this.selfAssessmentItemMapper = selfAssessmentItemMapper;
        this.selfAssessmentTaskMapper = selfAssessmentTaskMapper;
        this.selfAssessmentRecordMapper = selfAssessmentRecordMapper;
    }

    /**
     * 发布自评任务（含条目），事务保证数据一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SelfAssessmentTask> publishTask(Long teacherId, AssessmentTaskPublishVO publishVO) {
        // 1. 参数校验
        if (publishVO.getCourseId() == null || publishVO.getTaskTitle() == null || publishVO.getTaskTitle().trim().isEmpty()) {
            return Result.fail("课程ID和任务标题不能为空");
        }
        if (CollectionUtils.isEmpty(publishVO.getItemList())) {
            return Result.fail("自评条目不能为空");
        }

        // 2. 封装自评任务实体
        SelfAssessmentTask task = new SelfAssessmentTask();
        task.setTeacherId(teacherId);
        task.setCourseId(publishVO.getCourseId());
        task.setTaskTitle(publishVO.getTaskTitle().trim());
        task.setTaskDesc(publishVO.getTaskDesc() == null ? "" : publishVO.getTaskDesc().trim());
        task.setPublishTime(LocalDateTime.now());
        task.setEndTime(publishVO.getEndTime());
        task.setStatus("valid");

        // 3. 插入任务，获取自增ID
        this.save(task);
        Long taskId = task.getId();

        // 4. 批量插入自评条目
        List<SelfAssessmentItem> itemList = new ArrayList<>();
        for (AssessmentItemVO itemVO : publishVO.getItemList()) {
            SelfAssessmentItem item = new SelfAssessmentItem();
            item.setTaskId(taskId);
            item.setItemContent(itemVO.getItemContent().trim());
            item.setSortNum(itemVO.getSortNum() == null ? 0 : itemVO.getSortNum());
            itemList.add(item);
        }
        selfAssessmentItemMapper.batchInsert(itemList);

        return Result.success(task);
    }

    @Override
    public Result<PageInfo<SelfAssessmentTask>> getTeacherTaskList(Long teacherId, Integer pageNum, Integer pageSize) {
        // 默认分页参数
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;

        // 分页查询
        PageHelper.startPage(pageNum, pageSize);
        List<SelfAssessmentTask> taskList = baseMapper.selectByTeacherId(teacherId);
        PageInfo<SelfAssessmentTask> pageInfo = new PageInfo<>(taskList);

        for (SelfAssessmentTask task : taskList) {
            Long taskId = task.getId(); // 假设SelfAssessmentTask的主键id对应task_id字段，若字段名是taskId则用task.getTaskId()
            if (taskId != null) {
                // 查询去重的studentId列表（若无数据返回空列表，避免null）
                List<Long> studentIds = selfAssessmentRecordMapper.selectDistinctStudentIdsByTaskId(taskId);
                task.setStudentIds(studentIds == null ? Collections.emptyList() : studentIds);
            } else {
                // 异常情况：任务ID为空，赋值空列表
                task.setStudentIds(Collections.emptyList());
            }
        }

        return Result.success(pageInfo);
    }

    // 新增1：实现 /record/list 接口 - 分页查询学生自评记录
    @Override
    public Result<PageInfo<StudentRecordVO>> getStudentRecordList(Long studentId, Integer pageNum, Integer pageSize) {
        // 处理分页默认值（与你现有分页逻辑一致）
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;

        // 开启分页（复用 PageHelper，与现有代码一致）
        PageHelper.startPage(pageNum, pageSize);

        // 调用 Mapper 查询记录列表（关联任务、条目信息）
        List<StudentRecordVO> recordList = selfAssessmentRecordMapper.selectStudentRecordList(studentId);

        // 封装分页结果
        PageInfo<StudentRecordVO> pageInfo = new PageInfo<>(recordList);

        // 返回统一 Result 格式
        return Result.success("自评记录查询成功", pageInfo);
    }

    // 新增2：实现 /task/detail 接口 - 查询自评任务详情（含条目列表）
    @Override
    public Result<AssessmentTaskDetailVO> getTaskDetail(Long taskId, Long studentId) {
        // 1. 校验任务ID非空
        if (taskId == null) {
            return Result.fail("任务ID不能为空");
        }

        // 2. 查询任务基本信息（复用 MyBatis-Plus 通用方法）
        SelfAssessmentTask task = selfAssessmentTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.fail("该自评任务不存在或已删除");
        }

        // 3. 查询任务关联的条目列表
        List<StudentAssessmentItemVO> selfAssessmentRecords = selfAssessmentRecordMapper.selectByTaskAndStudent(taskId, studentId);

        // 4. 封装任务详情 VO（复用 BeanUtils，简化属性复制）
        AssessmentTaskDetailVO taskDetailVO = new AssessmentTaskDetailVO();
        BeanUtils.copyProperties(task, taskDetailVO);

        // 5. 设置条目列表
        taskDetailVO.setItemList(selfAssessmentRecords);

        // 6. 返回统一 Result 格式
        return Result.success("任务详情查询成功", taskDetailVO);
    }
}
