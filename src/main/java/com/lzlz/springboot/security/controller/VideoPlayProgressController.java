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
@Tag(name = "视频播放进度")
public class VideoPlayProgressController {

    @Resource
    private VideoPlayProgressService videoPlayProgressService;

    // ====================== 1. 保存/更新播放进度（退出时调用）======================
@PostMapping("/save")
public Result<Void> saveProgress(
        @RequestHeader("userId") Long userId,
        @Valid @RequestBody SaveVideoProgressDTO dto  // 直接接收你的格式
) {
    videoPlayProgressService.saveOrUpdateProgress(userId, dto);
    return Result.success();
}

    // ====================== 2. 查询播放进度（打开视频时调用）======================
@GetMapping("/get")
public Result<Integer> getProgress(
        @RequestHeader("userId") Long userId,
        @RequestParam String resourceId  // 这里也改成 String
) {
    Integer progress = videoPlayProgressService.getProgress(userId, resourceId);
    return Result.success(progress);
}
}
