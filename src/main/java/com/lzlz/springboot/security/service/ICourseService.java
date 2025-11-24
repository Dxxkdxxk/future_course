package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.dto.CourseCreateDto;
import com.lzlz.springboot.security.dto.CourseUpdateDto;
import com.lzlz.springboot.security.entity.Course;

import java.util.List;

/**
 * 课程服务的接口
 * 继承 IService 以便获得MP的批量操作支持
 */
public interface ICourseService extends IService<Course> {

    // (!!!) 新增的接口，对应 GET /api/v1/course
    List<Course> getAllCourses();

    // (!!!) 以下接口来自您原有的 CourseService.java
    Course getCourseById(Long id);

    Course createCourse(CourseCreateDto createDto);

    Course updateCourse(Long id, CourseUpdateDto updateDto);

    void deleteCourse(Long id);
}