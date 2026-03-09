package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.GraphMetadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GraphMetadataMapper extends BaseMapper<GraphMetadata> {

    // (!!!)
    // 无需手写 insert 方法。
    // BaseMapper<GraphMetadata> 自动提供了 insert()。
    //
}