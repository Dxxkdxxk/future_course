package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.SelfAssessmentTask;
import com.lzlz.springboot.security.entity.StudentAssessmentItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SelfAssessmentTaskMapper extends BaseMapper<SelfAssessmentTask> {
    // 按教师ID查询已发布任务
    List<SelfAssessmentTask> selectByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT id AS itemId, item_content AS itemContent FROM self_assessment_item WHERE task_id = #{taskId} ORDER BY sort_num ASC")
    List<StudentAssessmentItemVO> selectTaskItemList(@Param("taskId") Long taskId);

    // 按学生ID查询可参与的有效任务（未提交+未过期）
    List<SelfAssessmentTask> selectValidTaskForStudent(@Param("studentId") Long studentId);
}
