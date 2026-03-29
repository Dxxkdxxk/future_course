package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.CourseCreateDto;
import com.lzlz.springboot.security.dto.CourseUpdateDto;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.service.ICourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course")
public class CourseController {

    @Autowired
    private ICourseService courseService;

    // GET (All) - 200 OK
    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    // POST (Create) - 201 Created
    @PostMapping
    public ResponseEntity<ApiResponse<Course>> createCourse(@RequestBody CourseCreateDto createDto) {
        Course course = courseService.createCourse(createDto);

        // (!!!) 返回 201 Created 状态码
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(course));
    }

    // GET (One) - 200 OK
    // (404 由 GlobalExceptionHandler 自动处理)
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Course>> getCourse(@PathVariable("courseId") Long id) {
        Course course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success(course));
    }

    // PUT (Update) - 200 OK
    // (404 由 GlobalExceptionHandler 自动处理)
    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Course>> updateCourse(@PathVariable("courseId") Long id, @RequestBody CourseUpdateDto updateDto) {
        Course course = courseService.updateCourse(id, updateDto);
        return ResponseEntity.ok(ApiResponse.success(course));
    }

    @PostMapping("/{courseId}/teaching-plan")
    public ResponseEntity<ApiResponse<Course>> uploadTeachingPlan(
            @PathVariable("courseId") Long id,
            @RequestParam("file") MultipartFile file) {
        Course course = courseService.uploadTeachingPlan(id, file);
        return ResponseEntity.ok(ApiResponse.success(course));
    }

    // DELETE (Delete) - 204 No Content
    // (404 由 GlobalExceptionHandler 自动处理)
    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Object>> deleteCourse(@PathVariable("courseId") Long id) {
        courseService.deleteCourse(id);

        // 使用这个版本，它会返回 200 OK 和 统一的JSON结构
        return ResponseEntity.ok(ApiResponse.success());
    }
}
