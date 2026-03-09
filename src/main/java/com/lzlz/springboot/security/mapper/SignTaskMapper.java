package com.lzlz.springboot.security.mapper;

import com.lzlz.springboot.security.entity.SignTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface SignTaskMapper extends BaseMapper<SignTask> {
    // 按教师ID和条件查询签到任务（汇总统计）
    List<SignTask> selectSummaryByTeacher(
            @Param("teacherId") Long teacherId,
            @Param("courseId") Long courseId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}
