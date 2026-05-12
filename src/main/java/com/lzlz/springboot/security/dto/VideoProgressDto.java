package com.lzlz.springboot.security.dto;

import lombok.Data;

public class VideoProgressDto {

    @Data
    public static class ReportRequest {
        private String resourceId;
        private Integer watchedSeconds;
        private Integer durationSeconds;
    }

    @Data
    public static class ReportResponse {
        private String nodeId;
        private GraphNodeProgress progress;
    }
}

