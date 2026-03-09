package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.dto.StudentDto;
import com.lzlz.springboot.security.entity.CourseStudent;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseStudentService extends IService<CourseStudent> {

    // 手动添加单个学生
    void addStudent(Long courseId, StudentDto.CreateRequest request);

    // Excel 批量导入
    int importStudents(Long courseId, MultipartFile file);

    // 获取课程下的学生列表
    List<CourseStudent> getStudentsByCourse(Long courseId);
}