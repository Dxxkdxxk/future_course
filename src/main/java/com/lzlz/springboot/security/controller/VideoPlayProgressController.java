package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.SaveVideoProgressDTO;
import com.lzlz.springboot.security.response.Result;
import com.lzlz.springboot.security.service.VideoPlayProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/video/progress")
public class VideoPlayProgressController {

    @Resource
    private VideoPlayProgressService videoPlayProgressService;

    // ====================== 保存进度：路径参数 courseId ======================
    @PostMapping("/save/{courseId}")
    public Result<Void> saveProgress(
            @RequestHeader("userId") Long userId,
            @PathVariable Long courseId,  // 路径参数：课程ID
            @Valid @RequestBody SaveVideoProgressDTO dto
    ) {
        videoPlayProgressService.saveOrUpdateProgress(userId, courseId, dto);
        return Result.success();
    }

    // ====================== 查询进度：路径参数 courseId ======================
    @GetMapping("/get/{courseId}")
    public Result<Integer> getProgress(
            @RequestHeader("userId") Long userId,
            @PathVariable Long courseId,  // 路径参数：课程ID
            @RequestParam String resourceId
    ) {
        Integer progress = videoPlayProgressService.getProgress(userId, courseId, resourceId);
        return Result.success(progress);
    }
}
