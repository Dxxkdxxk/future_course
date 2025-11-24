package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzlz.springboot.security.dto.CourseCreateDto;
import com.lzlz.springboot.security.dto.CourseUpdateDto;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.CourseMapper;
import com.lzlz.springboot.security.service.ICourseService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    @Override
    public List<Course> getAllCourses() {
        return this.list();
    }

    @Override
    public Course getCourseById(Long id) {
        Course course = this.getById(id);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        return course;
    }

    @Override
    public Course createCourse(CourseCreateDto createDto) {
        Course course = new Course();

        course.setName(createDto.getName());
        course.setTeacherId(createDto.getTeacherId());
        course.setBackground(createDto.getBackground());
        course.setPosition(createDto.getPosition());
        course.setGoal(createDto.getGoal());
        course.setFeature(createDto.getFeature());
        course.setKnowledgeLogic(createDto.getKnowledgeLogic());
        course.setTeachingPlan(createDto.getTeachingPlan());
        course.setCreatedAt(LocalDateTime.now());

        this.save(course);
        return course;
    }

    // (!!!)
    // (!!!) 这里的逻辑已完全修正 (!!!)
    // (!!!)
    @Override
    public Course updateCourse(Long id, CourseUpdateDto updateDto) {
        // 1. 从数据库获取当前的完整数据
        Course course = this.getById(id);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }

        // 2. 检查 DTO 中的每个字段，如果不为 null，才更新
        //    (如果用户发送了 "" 空字符串，也会被更新，这是正确的)

        if (updateDto.getName() != null) {
            course.setName(updateDto.getName());
        }
        if (updateDto.getBackground() != null) {
            course.setBackground(updateDto.getBackground());
        }
        if (updateDto.getPosition() != null) {
            course.setPosition(updateDto.getPosition());
        }
        if (updateDto.getGoal() != null) {
            course.setGoal(updateDto.getGoal());
        }
        if (updateDto.getFeature() != null) {
            course.setFeature(updateDto.getFeature());
        }
        if (updateDto.getKnowledgeLogic() != null) {
            course.setKnowledgeLogic(updateDto.getKnowledgeLogic());
        }
        if (updateDto.getTeachingPlan() != null) {
            course.setTeachingPlan(updateDto.getTeachingPlan());
        }

        // 3. 保存被部分修改后的对象
        // 'updated_at' 字段由数据库自动更新
        this.updateById(course);
        return course;
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = this.getById(id);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        this.removeById(id);
    }
}