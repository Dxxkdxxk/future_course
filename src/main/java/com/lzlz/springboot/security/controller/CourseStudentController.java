package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.StudentDto;
import com.lzlz.springboot.security.entity.CourseStudent;
import com.lzlz.springboot.security.service.CourseStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/course/{courseId}/students")
public class CourseStudentController {

    @Autowired
    private CourseStudentService studentService;

    /**
     * 手动导入(添加)单个学生
     * POST /api/v1/courses/{courseId}/students
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> addStudent(
            @PathVariable Long courseId,
            @RequestBody StudentDto.CreateRequest request) {

        studentService.addStudent(courseId, request);

        // 使用 ApiResponse.success() 表示操作成功
        // 或者使用 created 状态码
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null)); // 成功且不返回额外数据
    }

    /**
     * 文件导入学生 (Excel)
     * POST /api/v1/courses/{courseId}/students/import
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importStudents(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {

        int count = studentService.importStudents(courseId, file);

        Map<String, Integer> data = new HashMap<>();
        data.put("count", count);

        ApiResponse<Map<String, Integer>> response = ApiResponse.created(data);
        response.setMsg("成功导入 " + count + " 名学生");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取班级学生列表
     * GET /api/v1/courses/{courseId}/students
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseStudent>>> getStudents(@PathVariable Long courseId) {
        List<CourseStudent> list = studentService.getStudentsByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}