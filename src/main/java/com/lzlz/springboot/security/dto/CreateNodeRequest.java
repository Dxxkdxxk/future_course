// 在你的 .dto 包中创建这个新文件
package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data // 使用 @Data (或 @Getter/@Setter) 自动生成方法
public class CreateNodeRequest {

    // 对应 "name": "1.2 发展历史"
    private String name;

    // 对应 "description": "..."
    private String description;

    // 对应 "label": "KnowledgeUnit"
    private String label;

    // 对应 "parentId": "node-a" (这是父节点的nodeId)
    private String parentId;
}