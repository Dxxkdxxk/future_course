package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_graphs") // (!!!) 1. 告诉MP表名
public class GraphMetadata {

    // (!!!) 2. 告诉MP这是自增主键 (匹配您要求的 bigint)
    @TableId(value = "graph_id", type = IdType.AUTO)
    private long graphId;

    private long courseId; // 匹配您要求的 bigint
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}