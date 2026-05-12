package com.lzlz.springboot.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNodeProgress {
    private Double videoProgress;
    private Double examProgress;
    private Double homeworkProgress;
    private Double overallProgress;
    private LocalDateTime updatedAt;
}

