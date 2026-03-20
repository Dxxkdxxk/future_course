package com.lzlz.springboot.security.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.TeacherOptionDto;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/teachers")
public class AdminTeacherController {

    @Autowired
    private UserMapper userMapper;

    @GetMapping
    public ApiResponse<List<TeacherOptionDto>> getAllTeachers() {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("role", "teacher");
        query.select("id", "username", "role");
        query.orderByAsc("id");

        List<TeacherOptionDto> list = userMapper.selectList(query).stream()
                .map(u -> new TeacherOptionDto(u.getId(), u.getUsername(), u.getRole()))
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }
}
