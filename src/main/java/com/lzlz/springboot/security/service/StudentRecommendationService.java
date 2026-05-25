package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.PageResponse;
import com.lzlz.springboot.security.dto.RecommendationResponse;

public interface StudentRecommendationService {
    PageResponse<RecommendationResponse> getRecommendations(Integer studentId,
                                                             Long courseId,
                                                             Long graphId,
                                                             String weakPointId,
                                                             Boolean isCompleted,
                                                             Integer page,
                                                             Integer pageSize);
}
