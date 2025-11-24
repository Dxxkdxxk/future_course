package com.lzlz.springboot.security.mapper; // (!!!) 注意包名已更新

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> { // (!!!) 继承 BaseMapper

    // (!!!) 删除所有自定义的SQL方法
    // BaseMapper 已经自动提供了 insert, delete, updateById, selectById 等所有功能。

}