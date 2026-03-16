package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lzlz.springboot.security.constants.RedisKeys;
import com.lzlz.springboot.security.entity.ClassStudent;
import com.lzlz.springboot.security.entity.CourseClass;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.mapper.ClassStudentMapper;
import com.lzlz.springboot.security.mapper.CourseClassMapper;
import com.lzlz.springboot.security.mapper.UserMapper;
import com.lzlz.springboot.security.service.IClassService;
import com.lzlz.springboot.security.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassServiceImpl implements IClassService {

    @Autowired
    private CourseClassMapper classMapper;

    @Autowired
    private ClassStudentMapper classStudentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Value("${cache.ttl.class-list-seconds:300}")
    private long classListTtlSeconds;

    @Override
    public CourseClass createClass(Long courseId, String name) {
        QueryWrapper<CourseClass> query = new QueryWrapper<>();
        query.eq("course_id", courseId).eq("name", name);
        if (classMapper.selectCount(query) > 0) {
            throw new CustomGraphException(400, "该课程下班级名称已存在");
        }

        CourseClass cc = new CourseClass();
        cc.setCourseId(courseId);
        cc.setName(name);
        classMapper.insert(cc);

        redisCacheService.delete(RedisKeys.classList(courseId));
        return cc;
    }

    @Override
    public List<CourseClass> getClassesByCourse(Long courseId) {
        String key = RedisKeys.classList(courseId);
        List<CourseClass> cached = redisCacheService.get(key, new TypeReference<List<CourseClass>>() {
        });
        if (cached != null) {
            return cached;
        }

        QueryWrapper<CourseClass> query = new QueryWrapper<>();
        query.eq("course_id", courseId).orderByDesc("created_at");
        List<CourseClass> result = classMapper.selectList(query);
        redisCacheService.set(key, result, Duration.ofSeconds(classListTtlSeconds));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudentsToClass(Long classId, List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        QueryWrapper<ClassStudent> query = new QueryWrapper<>();
        query.eq("class_id", classId);
        List<ClassStudent> existing = classStudentMapper.selectList(query);

        List<Integer> existingUserIds = existing.stream().map(ClassStudent::getUserId).collect(Collectors.toList());

        List<ClassStudent> toInsert = new ArrayList<>();
        for (Integer uid : userIds) {
            if (!existingUserIds.contains(uid)) {
                ClassStudent cs = new ClassStudent();
                cs.setClassId(classId);
                cs.setUserId(uid);
                toInsert.add(cs);
            }
        }

        for (ClassStudent cs : toInsert) {
            classStudentMapper.insert(cs);
        }
    }

    @Override
    public List<User> getStudentsInClass(Long classId) {
        QueryWrapper<ClassStudent> query = new QueryWrapper<>();
        query.eq("class_id", classId);
        List<ClassStudent> relations = classStudentMapper.selectList(query);

        if (relations.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> userIds = relations.stream().map(ClassStudent::getUserId).collect(Collectors.toList());
        return userMapper.selectBatchIds(userIds);
    }

    @Override
    public void removeStudentFromClass(Long classId, Integer userId) {
        QueryWrapper<ClassStudent> query = new QueryWrapper<>();
        query.eq("class_id", classId).eq("user_id", userId);
        classStudentMapper.delete(query);
    }
}
