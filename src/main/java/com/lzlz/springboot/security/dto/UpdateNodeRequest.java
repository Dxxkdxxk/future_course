package com.lzlz.springboot.security.dto;

import lombok.Data;

/**
 * 接收更新节点请求的 DTO
 * (对应 "PUT .../nodes/{nodeId}" 的请求体)
 * 字段是可选的；前端只发送它想修改的字段。
 */
@Data
public class UpdateNodeRequest {

    /**
     * 更新后的节点名称 (如果为 null，则不更新)
     */
    private String name;

    /**
     * 更新后的节点描述 (如果为 null，则不更新)
     */
    private String description;
}