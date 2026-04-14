package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.CreateHomeworkRequest;
import com.lzlz.springboot.security.dto.HomeworkDetailResponse;
import com.lzlz.springboot.security.dto.StudentHomeworkDetailDto;
import com.lzlz.springboot.security.entity.Homework;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HomeworkService {
    void createHomework(Long courseId, CreateHomeworkRequest request, MultipartFile[] files);

    List<Homework> getHomeworkList(Long courseId);

    List<StudentHomeworkDetailDto> getHomeworkListForStudent(Long courseId, Long studentId);

    HomeworkDetailResponse getHomeworkDetailForTeacher(Long courseId, Long homeworkId);

    StudentHomeworkDetailDto getHomeworkDetailForStudent(Long courseId, Long homeworkId, Long studentId);
}
