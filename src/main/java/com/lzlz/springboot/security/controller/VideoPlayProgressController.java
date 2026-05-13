package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.SaveVideoProgressDTO;
import com.lzlz.springboot.security.response.Result;
import com.lzlz.springboot.security.service.VideoPlayProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.lzlz.springboot.security.entity.VideoProgressVO;


@RestController
@RequestMapping("/api/video/progress")
public class VideoPlayProgressController {

    @Resource
    private VideoPlayProgressService videoPlayProgressService;

    @Autowired
    private CurrentUserResolver currentUserResolver;
    // ====================== 保存进度：路径参数 courseId ======================
    @PostMapping("/save/{courseId}")
    public Result<Void> saveProgress(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,  // 路径参数：课程ID
            @Valid @RequestBody SaveVideoProgressDTO dto
    ) {
        User currentUser = currentUserResolver.requireUser(user);
        videoPlayProgressService.saveOrUpdateProgress(currentUser.getId(), courseId, dto);
        return Result.success();
    }

    // ====================== 查询进度：路径参数 courseId ======================
@GetMapping("/get/{courseId}")
public Result<VideoProgressVO> getProgress(
       @AuthenticationPrincipal User user,
        @PathVariable Long courseId,
        @RequestParam String resourceId
) {
            User currentUser = currentUserResolver.requireUser(user);
    VideoProgressVO progress = videoPlayProgressService.getProgress((currentUser.getId(), courseId, resourceId);
    return Result.success(progress);
}
}
