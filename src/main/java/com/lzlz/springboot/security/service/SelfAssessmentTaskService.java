package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.entity.AssessmentTaskDetailVO;
import com.lzlz.springboot.security.entity.AssessmentTaskPublishVO;
import com.lzlz.springboot.security.entity.SelfAssessmentTask;
import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.entity.StudentRecordVO;
import com.lzlz.springboot.security.response.Result;

public interface SelfAssessmentTaskService extends IService<SelfAssessmentTask> {
    // 发布自评任务（含条目）
    Result<SelfAssessmentTask> publishTask(Long teacherId, AssessmentTaskPublishVO publishVO);

    // 查询教师已发布任务列表
    Result<PageInfo<SelfAssessmentTask>> getTeacherTaskList(Long teacherId, Integer pageNum, Integer pageSize);

    // 新增1：分页查询学生自评记录列表（适配 /record/list）
    Result<PageInfo<StudentRecordVO>> getStudentRecordList(Long studentId, Integer pageNum, Integer pageSize);

    // 查询任务详情（含条目）
    Result<AssessmentTaskDetailVO> getTaskDetail(Long taskId, Long studentId);
}
