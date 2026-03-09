package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class CreateEdgeRequest {
    // 注意：graphId 通常从 URL 路径变量获取，不需要放在 Body 里
    // 但如果你非要放在 Body 里也可以，不过为了 RESTful 风格，推荐只放以下三个：

    private String sourceNodeId;
    private String targetNodeId;

    /**
     * 关系类型，例如 "contains", "related_to"
     * 如果为空，后端可以设默认值
     */
    private String relationType;
}