package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.TestTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestTaskMapper extends BaseMapper<TestTask> {
}