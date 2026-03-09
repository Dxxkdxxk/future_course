package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.ExamFunctionDto;
import com.lzlz.springboot.security.security.User;
import com.lzlz.springboot.security.service.ExamFunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/course")
public class StudentCourseController {

    @Autowired
    private ExamFunctionService examFunctionService;

    /**
     * [修改] 根据课程ID查找我所在的班级
     * GET /api/v1/student/course/{courseId}/class
     * * 场景：学生点击"Java课程"卡片进入详情页时调用，
     * 获取到的 classId 将用于后续查询考试列表。
     */
    @GetMapping("/{courseId}/class")
    public ResponseEntity<ApiResponse<ExamFunctionDto.MyClassInfo>> getMyClassByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user) {

        // 注意：这里返回单个对象，因为正常情况下一个学生在一门课里只会在一个班级
        ExamFunctionDto.MyClassInfo info =
                examFunctionService.getStudentClassByCourse(user.getId(), courseId);

        return ResponseEntity.ok(new ApiResponse<>(200, "获取成功", info));
    }
}