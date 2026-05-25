package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.AbilityEvaluationResponse;
import com.lzlz.springboot.security.dto.KnowledgeEvaluationResponse;
import com.lzlz.springboot.security.dto.RadarChartResponse;

public interface StudentEvaluationService {
    KnowledgeEvaluationResponse getKnowledgeEvaluation(Integer studentId, Long courseId, Long graphId, Boolean isLatest);

    AbilityEvaluationResponse getAbilityEvaluation(Integer studentId, Long courseId, Long graphId, Boolean isLatest);

    RadarChartResponse getRadarChart(Integer studentId, Long courseId, Long graphId, String chartType, Boolean refresh);
}
