package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.dto.CourseCreateDto;
import com.lzlz.springboot.security.dto.CourseUpdateDto;
import com.lzlz.springboot.security.entity.Course;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ICourseService extends IService<Course> {

    List<Course> getAllCourses();

    List<Course> getCoursesByTeacherId(Integer teacherId);

    List<Course> getCoursesByStudentId(Integer studentId);

    Course getCourseById(Long id);

    Course createCourse(CourseCreateDto createDto);

    Course updateCourse(Long id, CourseUpdateDto updateDto);

    Course uploadTeachingPlan(Long id, MultipartFile file);

    void deleteCourse(Long id);
}
