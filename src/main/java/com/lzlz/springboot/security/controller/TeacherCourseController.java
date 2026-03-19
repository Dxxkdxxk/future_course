package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.CurrentUserResolver;
import com.lzlz.springboot.security.service.ICourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/course")
public class TeacherCourseController {

    @Autowired
    private ICourseService courseService;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getMyCourses(@AuthenticationPrincipal User user) {
        User currentUser = currentUserResolver.requireUser(user);
        List<Course> courses = courseService.getCoursesByTeacherId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(courses));
    }
}
