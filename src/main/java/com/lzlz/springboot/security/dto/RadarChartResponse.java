package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.List;

@Data
public class RadarChartResponse {
    private String chartType;
    private Long courseId;
    private Long graphId;
    private String snapshotTime;
    private List<DimensionItem> dimensions;

    @Data
    public static class DimensionItem {
        private String dimensionId;
        private String dimensionName;
        private Double dimensionValue;
        private Double maxValue;
        private Boolean hasChildren;
        private List<DimensionItem> children;
    }
}
