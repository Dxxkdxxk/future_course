package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.Annotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnotationMapper extends BaseMapper<Annotation> {

    /**
     * 根据学生ID、教材ID查询批注列表
     */
    List<Annotation> selectByStudentAndTextbook(
            @Param("studentId") Long studentId,
            @Param("resourceId") Long resourceId
    );
}
