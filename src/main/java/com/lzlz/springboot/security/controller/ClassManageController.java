package com.lzlz.springboot.security.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.SimpleUserDto;
import com.lzlz.springboot.security.entity.CourseClass;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.mapper.UserMapper;
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

    @GetMapping("/candidates")
    public ApiResponse<List<SimpleUserDto>> getCandidates() {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("role", "student");
        query.select("id", "username", "email", "role");
        List<User> users = userMapper.selectList(query);

        List<SimpleUserDto> dtos = users.stream()
                .map(u -> new SimpleUserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole()))
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    @PostMapping
    public ApiResponse<CourseClass> createClass(@RequestBody CreateClassRequest request) {
        CourseClass cc = classService.createClass(request.getName());
        return ApiResponse.success(cc);
    }

    @GetMapping
    public ApiResponse<List<CourseClass>> listClasses() {
        return ApiResponse.success(classService.getAllClasses());
    }

    @PostMapping("/{classId}/students")
    public ApiResponse<Void> addStudents(
            @PathVariable Long classId,
            @RequestBody AddStudentRequest request) {
        classService.addStudentsToClass(classId, request.getUserIds());
        return ApiResponse.success();
    }

    @GetMapping("/{classId}/students")
    public ApiResponse<List<SimpleUserDto>> getClassStudents(@PathVariable Long classId) {
        List<User> students = classService.getStudentsInClass(classId);
        List<SimpleUserDto> dtos = students.stream()
                .map(u -> new SimpleUserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole()))
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @DeleteMapping("/{classId}/students/{userId}")
    public ApiResponse<Void> removeStudent(@PathVariable Long classId, @PathVariable Integer userId) {
        classService.removeStudentFromClass(classId, userId);
        return ApiResponse.success();
    }

    @Data
    public static class CreateClassRequest {
        private String name;
    }

    @Data
    public static class AddStudentRequest {
        private List<Integer> userIds;
    }
}
