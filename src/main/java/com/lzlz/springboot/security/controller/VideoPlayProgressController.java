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

    // 保存进度接口（适配前端JSON）
    @PostMapping("/save")
    public Result<Void> saveProgress(
            @RequestHeader("userId") Long userId,
            @Valid @RequestBody SaveVideoProgressDTO dto
    ) {
        videoPlayProgressService.saveOrUpdateProgress(userId, dto);
        return Result.success();
    }

    // 查询进度接口（参数严格用 String）
    @GetMapping("/get")
    public Result<Integer> getProgress(
            @RequestHeader("userId") Long userId,
            // 这里必须是 String，对应前端 resourceId: res-123
            @RequestParam String resourceId
    ) {
        Integer progress = videoPlayProgressService.getProgress(userId, resourceId);
        return Result.success(progress);
    }
}
