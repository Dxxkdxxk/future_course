package com.lzlz.springboot.security.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GraphEdge {
    private String edgeId;
    private String sourceNodeId;
    private String targetNodeId;
    private String relationType;
}