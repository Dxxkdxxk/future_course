package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.ExtendReading;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtendReadingMapper extends BaseMapper<ExtendReading> {

    /**
     * 根据学生ID、教材ID、章节ID查询拓展阅读列表
     */
    List<ExtendReading> selectByStudentAndTextbookAndChapter(
            @Param("studentId") String studentId,
            @Param("textbookId") Long textbookId,
            @Param("chapterId") Long chapterId);
}
