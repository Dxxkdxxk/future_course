package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.entity.Annotation;
import com.lzlz.springboot.security.response.AnnotationAddRequest;
import com.lzlz.springboot.security.response.AnnotationResponse;


import java.util.List;

public interface AnnotationService extends IService<Annotation> {
    Long addAnnotation(AnnotationAddRequest request, Long id);
    List<AnnotationResponse> getAnnotationList(Long studentId, Long resourceId);
}
