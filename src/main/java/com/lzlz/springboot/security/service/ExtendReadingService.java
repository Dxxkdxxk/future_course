package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.entity.ExtendReading;
import com.lzlz.springboot.security.response.ExtendReadingAddRequest;
import com.lzlz.springboot.security.response.ExtendReadingResponse;


import java.util.List;

public interface ExtendReadingService extends IService<ExtendReading> {
    Long addExtendReading(ExtendReadingAddRequest request);
    List<ExtendReadingResponse> getExtendReadingList(String studentId, Long textbookId, Long chapterId);
    boolean deleteExtendReading(Long id, String studentId);
}

