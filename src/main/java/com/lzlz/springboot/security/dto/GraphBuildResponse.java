package com.lzlz.springboot.security.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GraphBuildResponse {
    private long graphId;
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
}