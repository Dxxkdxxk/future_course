package com.lzlz.springboot.security.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GraphNode {
    private String nodeId;
    private String name;
    private String description;
    private String label; // "KnowledgeModule", "KnowledgeUnit", "KnowledgePoint"
}