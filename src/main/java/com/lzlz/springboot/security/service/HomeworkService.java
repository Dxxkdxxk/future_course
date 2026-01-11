package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;

public interface HomeworkService {
    void createHomework(Long courseId, CreateHomeworkRequest request);

    // (!!!) 新增接口定义
    HomeworkDetailResponse getHomeworkDetailForTeacher(Long homeworkId);
}