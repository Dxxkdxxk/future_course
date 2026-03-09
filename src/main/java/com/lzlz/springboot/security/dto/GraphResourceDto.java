package com.lzlz.springboot.security.dto;

import lombok.Data;

public class GraphResourceDto {

    @Data
    public static class BindRequest {
        private String fileUrl;
        private String name;
        // (!!!) 新增字段
        private String materialType;

        private String fileSize;
        private Boolean isVideo;
        private String description;
    }

    @Data
    public static class ResourceView {
        private String resourceId;
        private String name;
        // (!!!) 新增字段
        private String materialType;

        private String url;
        private String fileSize;
        private Boolean isVideo;
        private String description;
    }
}