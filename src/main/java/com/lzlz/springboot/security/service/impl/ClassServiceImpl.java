package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.entity.ClassStudent;
import com.lzlz.springboot.security.entity.CourseClass;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.exception.CustomGraphException; // 复用你的异常类
import com.lzlz.springboot.security.mapper.ClassStudentMapper;
import com.lzlz.springboot.security.mapper.CourseClassMapper;
import com.lzlz.springboot.security.mapper.UserMapper;

import com.lzlz.springboot.security.service.IClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public CourseClass createClass(Long courseId, String name) {
        // 简单重名校验
        QueryWrapper<CourseClass> query = new QueryWrapper<>();
        query.eq("course_id", courseId).eq("name", name);
        if (classMapper.selectCount(query) > 0) {
            throw new CustomGraphException(400, "该课程下已存在同名班级");
        }

        CourseClass cc = new CourseClass();
        cc.setCourseId(courseId);
        cc.setName(name);
        classMapper.insert(cc);
        return cc;
    }

    @Override
    public List<CourseClass> getClassesByCourse(Long courseId) {
        QueryWrapper<CourseClass> query = new QueryWrapper<>();
        query.eq("course_id", courseId).orderByDesc("created_at");
        return classMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudentsToClass(Long classId, List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) return;

        // 1. 查出该班级现有的学生ID，防止重复添加
        QueryWrapper<ClassStudent> query = new QueryWrapper<>();
        query.eq("class_id", classId);
        List<ClassStudent> existing = classStudentMapper.selectList(query);

        List<Integer> existingUserIds = existing.stream()
                .map(ClassStudent::getUserId)
                .collect(Collectors.toList());

        // 2. 过滤并插入
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
        // 1. 先查关联表拿到 ID 列表
        QueryWrapper<ClassStudent> query = new QueryWrapper<>();
        query.eq("class_id", classId);
        List<ClassStudent> relations = classStudentMapper.selectList(query);

        if (relations.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> userIds = relations.stream()
                .map(ClassStudent::getUserId)
                .collect(Collectors.toList());

        // 2. 查 User 表 (MyBatis-Plus 自动处理 WHERE id IN (...))
        // 处于隐私考虑，这里可能需要将 User 转换为 DTO，但在 Service 层先返回 User
        return userMapper.selectBatchIds(userIds);
    }

    @Override
    public void removeStudentFromClass(Long classId, Integer userId) {
        QueryWrapper<ClassStudent> query = new QueryWrapper<>();
        query.eq("class_id", classId).eq("user_id", userId);
        classStudentMapper.delete(query);
    }
}