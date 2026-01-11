package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.entity.Homework;

import java.util.List;

public interface HomeworkService {
    void createHomework(Long courseId, CreateHomeworkRequest request);

    // 教师端：获取所有作业（包括草稿）
    List<Homework> getHomeworkList(Long courseId);

    // (新增) 学生端：获取已发布的作业列表
    List<Homework> getHomeworkListForStudent(Long courseId);

    HomeworkDetailResponse getHomeworkDetailForTeacher(Long homeworkId);
}