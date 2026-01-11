package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.Homework;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HomeworkMapper extends BaseMapper<Homework> {
}