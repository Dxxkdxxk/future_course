package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.QuestionDto;
import com.lzlz.springboot.security.entity.Question;
import com.lzlz.springboot.security.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
// 路径: /api/v1/courses/{courseId}/questions
@RequestMapping("/api/v1/course/{courseId}/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * 单个添加题目
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createQuestion(
            @PathVariable Long courseId,
            @RequestBody QuestionDto.CreateRequest request) {

        String questionId = questionService.createQuestion(courseId, request);

        Map<String, String> data = new HashMap<>();
        data.put("id", questionId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    /**
     * Excel 批量导入
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importQuestions(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {

        int count = questionService.importQuestions(courseId, file);

        Map<String, Integer> data = new HashMap<>();
        data.put("count", count);

        ApiResponse<Map<String, Integer>> response = ApiResponse.created(data);
        response.setMsg("成功导入 " + count + " 道题目");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取题目列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Question>>> getQuestions(
            @PathVariable Long courseId,
            QuestionDto.QueryRequest queryRequest) {

        List<Question> list = questionService.getQuestions(courseId, queryRequest);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}