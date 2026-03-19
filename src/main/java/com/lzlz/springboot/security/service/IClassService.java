package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.entity.CourseClass;
import com.lzlz.springboot.security.entity.User;

import java.util.List;

public interface IClassService {
    // 创建班级
    CourseClass createClass(String name);

    // 获取某课程下的所有班级
    List<CourseClass> getAllClasses();

    // 批量添加学生到班级
    void addStudentsToClass(Long classId, List<Integer> userIds);

    // 获取某班级的所有学生详情
    List<User> getStudentsInClass(Long classId);

    // 从班级移除学生
    void removeStudentFromClass(Long classId, Integer userId);
}
