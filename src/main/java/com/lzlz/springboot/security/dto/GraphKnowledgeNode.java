package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class GraphKnowledgeNode {
    private String nodeId;
    private String parentNodeId;
    private String name;
    private String label;
    private String description;
}
