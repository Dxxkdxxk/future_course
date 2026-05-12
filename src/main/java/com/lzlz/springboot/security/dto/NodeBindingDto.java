package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.List;

public class NodeBindingDto {

    @Data
    public static class UpsertRequest {
        private String taskType; // EXAM | HOMEWORK
        private Long taskId;
        private Double weight;
    }

    @Data
    public static class RemoveRequest {
        private String taskType; // EXAM | HOMEWORK
        private Long taskId;
    }

    @Data
    public static class BindingItem {
        private String taskType;
        private Long taskId;
        private Double weight;
    }

    @Data
    public static class BindingListResponse {
        private String nodeId;
        private List<BindingItem> bindings;
    }
}

