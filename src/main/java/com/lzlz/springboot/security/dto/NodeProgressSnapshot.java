package com.lzlz.springboot.security.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeProgressSnapshot {
    private Double videoProgress;
    private Double examProgress;
    private Double homeworkProgress;
    private Double overallProgress;
}
