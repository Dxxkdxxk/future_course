package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("knowledge_ability_mappings")
public class KnowledgeAbilityMapping {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("graph_id")
    private Long graphId;

    @TableField("node_id")
    private String nodeId;

    @TableField("ability_point_id")
    private Long abilityPointId;

    @TableField("contribution_weight")
    private Double contributionWeight;
}
