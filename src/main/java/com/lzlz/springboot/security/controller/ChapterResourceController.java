package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.ApiResponse;
import com.lzlz.springboot.security.dto.ResourceDto;
import com.lzlz.springboot.security.service.ChapterResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course/{courseId}")
public class ChapterResourceController {

    @Autowired
    private ChapterResourceService resourceService;

    /**
     * 上传资源信息 (保存 URL)
     * 请求方式: POST (JSON Body)
     */
    @PostMapping("/chapter/{chapterId}/upload")
    public ResponseEntity<ApiResponse<Void>> uploadResource(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @RequestBody ResourceDto.UploadRequest request) { // (!!!) 改为 @RequestBody

        resourceService.saveResource(courseId, chapterId, request);

        // 返回成功
        return ResponseEntity.ok(new ApiResponse<>(0, "保存成功", null));
    }

    /**
     * 获取资源列表
     * GET /api/v1/course/{courseId}/chapter/{chapterId}/resource
     */
    @GetMapping("/chapter/{chapterId}/resource")
    public ResponseEntity<ApiResponse<List<ResourceDto.ResourceView>>> getResources(@PathVariable Long courseId, @PathVariable Long chapterId) {

        // (!!!) 修改点：将 courseId 也传进去
        List<ResourceDto.ResourceView> list = resourceService.getResources(courseId, chapterId);

        return ResponseEntity.ok(new ApiResponse<>(0, "获取成功", list));
    }

    // URL: POST /api/v1/course/{courseId}/resource
    @PostMapping("/resource")
    public
    ResponseEntity<ApiResponse<Void>> uploadCourseResource(@PathVariable Long courseId, @RequestBody ResourceDto.UploadRequest request) {

        // (!!!) 核心点：在这里硬编码 "0L"，复用 Service 逻辑
        // 0L 代表这是一个“课程级资源”
        resourceService.saveResource(courseId, 0L, request);

        return ResponseEntity.ok(new ApiResponse<>(0, "保存成功", null));
    }

    // (!!!) 新增：获取课程资源列表
    @GetMapping("/resource")
    public
    ResponseEntity<ApiResponse<List<ResourceDto.ResourceView>>> getCourseResources(@PathVariable Long courseId) {

        // 复用 Service，查询 chapterId = 0 的数据
        List<ResourceDto.ResourceView> list = resourceService.getResources(courseId, 0L);

        return ResponseEntity.ok(new ApiResponse<>(0, "获取成功", list));
    }
}