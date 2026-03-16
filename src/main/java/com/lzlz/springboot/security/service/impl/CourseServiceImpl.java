package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lzlz.springboot.security.constants.RedisKeys;
import com.lzlz.springboot.security.dto.CourseCreateDto;
import com.lzlz.springboot.security.dto.CourseUpdateDto;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.CourseMapper;
import com.lzlz.springboot.security.service.ICourseService;
import com.lzlz.springboot.security.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    private final RedisCacheService redisCacheService;

    @Value("${cache.ttl.course-list-seconds:300}")
    private long courseListTtlSeconds;

    @Value("${cache.ttl.course-detail-seconds:300}")
    private long courseDetailTtlSeconds;

    public CourseServiceImpl(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @Override
    public List<Course> getAllCourses() {
        String key = RedisKeys.courseList();
        List<Course> cached = redisCacheService.get(key, new TypeReference<List<Course>>() {
        });
        if (cached != null) {
            return cached;
        }

        List<Course> result = this.list();
        redisCacheService.set(key, result, Duration.ofSeconds(courseListTtlSeconds));
        return result;
    }

    @Override
    public Course getCourseById(Long id) {
        String key = RedisKeys.courseDetail(id);
        Course cached = redisCacheService.get(key, Course.class);
        if (cached != null) {
            return cached;
        }

        Course course = this.getById(id);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }

        redisCacheService.set(key, course, Duration.ofSeconds(courseDetailTtlSeconds));
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
        redisCacheService.delete(RedisKeys.courseList());
        return course;
    }

    @Override
    public Course updateCourse(Long id, CourseUpdateDto updateDto) {
        Course course = this.getById(id);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }

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

        this.updateById(course);
        redisCacheService.delete(RedisKeys.courseList());
        redisCacheService.delete(RedisKeys.courseDetail(id));
        return course;
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = this.getById(id);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }

        this.removeById(id);
        redisCacheService.delete(RedisKeys.courseList());
        redisCacheService.delete(RedisKeys.courseDetail(id));
    }
}
