package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class UpdateEdgeRequest {

    /**
     * 新的源节点ID (如果不传，则保持不变)
     */
    private String sourceNodeId;

    /**
     * 新的目标节点ID (如果不传，则保持不变)
     */
    private String targetNodeId;

    /**
     * 新的关系类型 (如果不传，则保持不变)
     */
    private String relationType;
}