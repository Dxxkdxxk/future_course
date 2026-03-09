package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.SignRecord;
import com.lzlz.springboot.security.entity.SignRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SignRecordMapper extends BaseMapper<SignRecord> {
    // 按签到任务ID查询签到明细（关联学生表）
    List<SignRecordVO> selectRecordVOByTaskId(@Param("signTaskId") Long signTaskId);

    // 判断学生是否已签到
    Integer countByTaskAndStudent(
            @Param("signTaskId") Long signTaskId,
            @Param("studentId") Long studentId
    );
}
