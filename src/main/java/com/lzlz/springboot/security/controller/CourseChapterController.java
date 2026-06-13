package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.CourseLearningChapterDto;
import com.lzlz.springboot.security.dto.GenerateTestDto;
import com.lzlz.springboot.security.dto.ResourceDto;
import com.lzlz.springboot.security.service.ChapterResourceService;
import com.lzlz.springboot.security.service.ChapterTestGeneratorService;
import com.lzlz.springboot.security.service.CourseLearningChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/course/{courseId}")
public class CourseChapterController {

    @Autowired
    private CourseLearningChapterService courseLearningChapterService;

    @Autowired
    private ChapterResourceService resourceService;

    @Autowired
    private ChapterTestGeneratorService generatorService;

    @PostMapping("/chapter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadChapterFile(
            @PathVariable Long courseId,
            @RequestParam("chapterFile") MultipartFile chapterFile) {

        int count = courseLearningChapterService.replaceByXlsx(courseId, chapterFile);
        Map<String, Object> data = new HashMap<>();
        data.put("chapterCount", count);
        data.put("fileName", chapterFile == null ? null : chapterFile.getOriginalFilename());
        return ResponseEntity.ok(new ApiResponse<>(0, "绔犺妭瑙ｆ瀽鎴愬姛", data));
    }

    @GetMapping("/chapters")
    public ResponseEntity<ApiResponse<CourseLearningChapterDto.ChapterListData>> getChapters(
            @PathVariable Long courseId) {
        CourseLearningChapterDto.ChapterListData data = courseLearningChapterService.getChapterList(courseId);
        return ResponseEntity.ok(new ApiResponse<>(0, "success", data));
    }

    @PostMapping("/chapter/{chapterId}/upload")
    public ResponseEntity<ApiResponse<Void>> uploadResource(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @RequestBody ResourceDto.UploadRequest request) {

        resourceService.saveResource(courseId, chapterId, request);
        return ResponseEntity.ok(new ApiResponse<>(0, "淇濆瓨鎴愬姛", null));
    }

    @GetMapping("/chapter/{chapterId}/resource")
    public ResponseEntity<ApiResponse<List<ResourceDto.ResourceView>>> getResources(
            @PathVariable Long courseId,
            @PathVariable Long chapterId) {

        List<ResourceDto.ResourceView> list = resourceService.getResources(courseId, chapterId);
        return ResponseEntity.ok(new ApiResponse<>(0, "鑾峰彇鎴愬姛", list));
    }

    @PostMapping("/resource")
    public ResponseEntity<ApiResponse<Void>> uploadCourseResource(
            @PathVariable Long courseId,
            @RequestBody ResourceDto.UploadRequest request) {

        resourceService.saveResource(courseId, 0L, request);
        return ResponseEntity.ok(new ApiResponse<>(0, "淇濆瓨鎴愬姛", null));
    }

    @GetMapping("/resource")
    public ResponseEntity<ApiResponse<List<ResourceDto.ResourceView>>> getCourseResources(
            @PathVariable Long courseId) {

        List<ResourceDto.ResourceView> list = resourceService.getResources(courseId, 0L);
        return ResponseEntity.ok(new ApiResponse<>(0, "鑾峰彇鎴愬姛", list));
    }

    @PostMapping("/chapter/test/{chapterId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> generateTest(
            @PathVariable("courseId") String courseId,
            @PathVariable("chapterId") String chapterId,
            @RequestBody GenerateTestDto.Request request) {

        Map<String, Long> result = generatorService.generateTest(courseId, chapterId, request);
        return ResponseEntity.ok(new ApiResponse<>(0, "璁剧疆鎴愬姛", result));
    }

    @GetMapping("/chapter/test/{chapterId}")
    public ResponseEntity<ApiResponse<GenerateTestDto.Request>> getChapterTest(
            @PathVariable("courseId") String courseId,
            @PathVariable("chapterId") String chapterId) {

        GenerateTestDto.Request data = generatorService.getTestDetails(chapterId);
        ApiResponse<GenerateTestDto.Request> response = new ApiResponse<>(0, "鑾峰彇鎴愬姛", data);
        return ResponseEntity.ok(response);
    }
}
