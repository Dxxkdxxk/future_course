package com.lzlz.springboot.security.dto;

import lombok.Data;

public class GraphWeightConfigDto {

    @Data
    public static class UpsertRequest {
        private String mode;
        private Double videoGlobal;
        private Double homeworkGlobal;
        private Double examGlobal;
    }

    @Data
    public static class WeightConfigResponse {
        private Long courseId;
        private Long graphId;
        private String mode;
        private Double videoGlobal;
        private Double homeworkGlobal;
        private Double examGlobal;
        private Long updatedBy;
        private String updatedAt;
    }
}

