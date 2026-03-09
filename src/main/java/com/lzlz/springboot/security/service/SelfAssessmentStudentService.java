package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.entity.SelfAssessmentItem;
import com.lzlz.springboot.security.entity.SelfAssessmentRecord;
import com.lzlz.springboot.security.entity.SelfAssessmentTask;
import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.entity.StudentAssessmentSubmitVO;
import com.lzlz.springboot.security.response.Result;

import java.util.List;

/**
 * 学生自评 Service 接口
 */
public interface SelfAssessmentStudentService extends IService<SelfAssessmentRecord> {
    // 学生提交自评结果
    Result<Void> submitAssessment(Long studentId, StudentAssessmentSubmitVO submitVO);

    Result<List<SelfAssessmentItem>> getTaskDetail(Long taskId);
    // 查询学生可参与的有效自评任务
    Result<PageInfo<SelfAssessmentTask>> getValidTaskList(Long studentId, Integer pageNum, Integer pageSize);
}
