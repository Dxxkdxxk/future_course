package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.CourseLearningChapterDto;
import com.lzlz.springboot.security.service.CourseLearningChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/course/{courseId}")
public class CourseLearningChapterController {

    private final CourseLearningChapterService courseLearningChapterService;

    @PostMapping("/chapter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadChapterFile(
            @PathVariable Long courseId,
            @RequestParam("chapterFile") MultipartFile chapterFile) {

        int count = courseLearningChapterService.replaceByXlsx(courseId, chapterFile);
        Map<String, Object> data = new HashMap<>();
        data.put("chapterCount", count);
        data.put("fileName", chapterFile == null ? null : chapterFile.getOriginalFilename());
        return ResponseEntity.ok(new ApiResponse<>(0, "章节解析成功", data));
    }

    @GetMapping("/chapters")
    public ResponseEntity<ApiResponse<CourseLearningChapterDto.ChapterListData>> getChapters(
            @PathVariable Long courseId) {
        CourseLearningChapterDto.ChapterListData data = courseLearningChapterService.getChapterList(courseId);
        return ResponseEntity.ok(new ApiResponse<>(0, "success", data));
    }
}
