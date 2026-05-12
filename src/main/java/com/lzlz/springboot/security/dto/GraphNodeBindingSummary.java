package com.lzlz.springboot.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNodeBindingSummary {
    private Integer videoCount;
    private Integer examCount;
    private Integer homeworkCount;
}

