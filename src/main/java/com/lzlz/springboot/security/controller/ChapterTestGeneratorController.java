package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.GenerateTestDto;
import com.lzlz.springboot.security.service.ChapterTestGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ChapterTestGeneratorController {

    @Autowired
    private ChapterTestGeneratorService generatorService;

    /**
     * 生成测试 (POST)
     * (保留之前的代码)
     */
    @PostMapping("/api/v1/course/{courseId}/chapter/test/{chapterId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> generateTest(
            @PathVariable("courseId") String courseId,
            @PathVariable("chapterId") String chapterId,
            @RequestBody GenerateTestDto.Request request) {

        Map<String, Long> result = generatorService.generateTest(courseId, chapterId, request);
        return ResponseEntity.ok(new ApiResponse<>(0, "设置成功", result));
    }

    /**
     * (!!!) 新增接口：获取某个章节下的测试 (GET)
     * URL: /api/v1/course/{courseId}/chapter/test/{chapterId}
     */
    @GetMapping("/api/v1/course/{courseId}/chapter/test/{chapterId}")
    public ResponseEntity<ApiResponse<GenerateTestDto.Request>> getChapterTest(
            @PathVariable("courseId") String courseId,
            @PathVariable("chapterId") String chapterId) {

        // 1. 调用 Service 查询详情
        GenerateTestDto.Request data = generatorService.getTestDetails(chapterId);

        // 2. 构造返回 (匹配 API 文档结构)
        // code: 0, msg: "获取成功"
        ApiResponse<GenerateTestDto.Request> response = new ApiResponse<>(0, "获取成功", data);

        return ResponseEntity.ok(response);
    }
}