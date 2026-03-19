package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lzlz.springboot.security.constants.RedisKeys;
import com.lzlz.springboot.security.dto.CourseCreateDto;
import com.lzlz.springboot.security.dto.CourseUpdateDto;
import com.lzlz.springboot.security.entity.ClassStudent;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.ClassStudentMapper;
import com.lzlz.springboot.security.mapper.CourseClassMapper;
import com.lzlz.springboot.security.mapper.CourseMapper;
import com.lzlz.springboot.security.service.ICourseService;
import com.lzlz.springboot.security.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    private final RedisCacheService redisCacheService;
    private final CourseClassMapper courseClassMapper;
    private final ClassStudentMapper classStudentMapper;

    @Value("${cache.ttl.course-list-seconds:300}")
    private long courseListTtlSeconds;

    @Value("${cache.ttl.course-detail-seconds:300}")
    private long courseDetailTtlSeconds;

    public CourseServiceImpl(RedisCacheService redisCacheService,
                             CourseClassMapper courseClassMapper,
                             ClassStudentMapper classStudentMapper) {
        this.redisCacheService = redisCacheService;
        this.courseClassMapper = courseClassMapper;
        this.classStudentMapper = classStudentMapper;
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
    public List<Course> getCoursesByTeacherId(Integer teacherId) {
        QueryWrapper<Course> wrapper = new QueryWrapper<>();
        wrapper.eq("teacher_id", teacherId).orderByDesc("created_at");
        return this.list(wrapper);
    }

    @Override
    public List<Course> getCoursesByStudentId(Integer studentId) {
        QueryWrapper<ClassStudent> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("user_id", studentId);
        List<ClassStudent> relations = classStudentMapper.selectList(relationWrapper);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> classIds = relations.stream()
                .map(ClassStudent::getClassId)
                .distinct()
                .collect(Collectors.toList());
        if (classIds.isEmpty()) {
            return Collections.emptyList();
        }

        QueryWrapper<Course> wrapper = new QueryWrapper<>();
        wrapper.in("class_id", classIds).orderByDesc("created_at");
        return this.list(wrapper);
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
        if (createDto.getClassId() == null || courseClassMapper.selectById(createDto.getClassId()) == null) {
            throw new ResourceNotFoundException("Class not found with id: " + createDto.getClassId());
        }

        Course course = new Course();
        course.setName(createDto.getName());
        course.setTeacherId(createDto.getTeacherId());
        course.setClassId(createDto.getClassId());
        course.setCourseNo(createDto.getCourseNo());
        course.setTerm(createDto.getTerm());
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
        if (updateDto.getCourseNo() != null) {
            course.setCourseNo(updateDto.getCourseNo());
        }
        if (updateDto.getTerm() != null) {
            course.setTerm(updateDto.getTerm());
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
