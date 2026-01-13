package com.lzlz.springboot.security.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.SimpleUserDto;
import com.lzlz.springboot.security.entity.CourseClass;
import com.lzlz.springboot.security.mapper.UserMapper;
import com.lzlz.springboot.security.security.User;
import com.lzlz.springboot.security.service.IClassService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teacher/class")
public class ClassManageController {

    @Autowired
    private IClassService classService;

    @Autowired
    private UserMapper userMapper;

    // ==========================================
    // 1. 基础数据获取
    // ==========================================

    // 1. 获取所有候选人 (修改返回类型)
    @GetMapping("/candidates")
    public
    ApiResponse<List<SimpleUserDto>> getCandidates() {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("role","student");
        query.select("id", "username", "email", "role");
        List<User> users = userMapper.selectList(query);

        // (!!!) 转换逻辑: User -> SimpleUserDto
        List<SimpleUserDto> dtos = users.stream().map(
                u -> new SimpleUserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole()))
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    // ==========================================
    // 2. 班级管理
    // ==========================================

    /**
     * 在某课程下创建班级
     */
    @PostMapping("/course/{courseId}")
    public ApiResponse<CourseClass> createClass(
            @PathVariable Long courseId,
            @RequestBody CreateClassRequest request) {
        CourseClass cc = classService.createClass(courseId, request.getName());
        return ApiResponse.success(cc);
    }

    /**
     * 获取某课程下的班级列表
     */
    @GetMapping("/course/{courseId}")
    public ApiResponse<List<CourseClass>> listClasses(@PathVariable Long courseId) {
        return ApiResponse.success(classService.getClassesByCourse(courseId));
    }

    // ==========================================
    // 3. 成员管理
    // ==========================================

    /**
     * 批量添加学生到班级
     */
    @PostMapping("/{classId}/students")
    public ApiResponse<Void> addStudents(
            @PathVariable Long classId,
            @RequestBody AddStudentRequest request) {
        classService.addStudentsToClass(classId, request.getUserIds());
        return ApiResponse.success();
    }

    // 5. 获取班级学生 (修改返回类型)
    @GetMapping("/{classId}/students")
    public ApiResponse<List<SimpleUserDto>> getClassStudents(@PathVariable Long classId) {
        // Service层现在返回的是 User 实体列表
        List<User> students = classService.getStudentsInClass(classId);

        // (!!!) 转换逻辑
        List<SimpleUserDto> dtos = students.stream().map(
                u -> new SimpleUserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole()))
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    /**
     * 移除班级内的某个学生
     */
    @DeleteMapping("/{classId}/students/{userId}")
    public ApiResponse<Void> removeStudent(@PathVariable Long classId, @PathVariable Integer userId) {
        classService.removeStudentFromClass(classId, userId);
        return ApiResponse.success();
    }

    // ==========================================
    // DTO 内部类
    // ==========================================

    @Data
    public static class CreateClassRequest {
        private String name;
    }

    @Data
    public static class AddStudentRequest {
        private List<Integer> userIds;
    }
}