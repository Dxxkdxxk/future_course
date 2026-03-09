package com.lzlz.springboot.security.controller;


import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import com.lzlz.springboot.security.response.AnnotationAddRequest;
import com.lzlz.springboot.security.response.AnnotationResponse;
import com.lzlz.springboot.security.response.Result;
import com.lzlz.springboot.security.security.CustomUserDetailsService;
import com.lzlz.springboot.security.service.AnnotationService;
import com.lzlz.springboot.security.service.MinIOService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/annotation")
public class AnnotationController {

    @Autowired
    private AnnotationService annotationService;

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Resource
    private MinIOService minIOService;
    /**
     * 添加批注（基本+扩展）
     */
    @PostMapping
    public Result<Map<String, Long>> addAnnotation(
            HttpServletRequest httpRequest,
            @Validated @ModelAttribute AnnotationAddRequest request,  // 表单绑定对象
            @RequestParam(value = "uploadFile", required = false) MultipartFile uploadFile  // 接收文件 // 接收上传文件
    ) {
        try {
            if (uploadFile != null && !uploadFile.isEmpty()) {
                String objectName = minIOService.uploadFile(uploadFile);
                // 给request填充MinIO相关字段（后续存入数据库）
                request.setMinioObjectName(objectName);
            }
            String token = jwtTokenProvider.resolveToken(httpRequest);
            String username = jwtTokenProvider.getUsername(token);
            User user = (User) userDetailsService.loadUserByUsername(username);
            Long annotationId = annotationService.addAnnotation(request, Long.valueOf(user.getId()));
            Map<String, Long> data = new HashMap<>();
            data.put("annotationId", annotationId);
            return Result.success("批注添加成功", data);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            return Result.fail("批注添加失败：" + e.getMessage());
        }
    }

    /**
     * 查询批注列表
     */
    @GetMapping("")
    public Result<List<AnnotationResponse>> getAnnotations(
            HttpServletRequest httpRequest,
            @RequestParam Long resourceId) {
        try {
            String token = jwtTokenProvider.resolveToken(httpRequest);
            String username = jwtTokenProvider.getUsername(token);
            User user = (User) userDetailsService.loadUserByUsername(username);
            List<AnnotationResponse> list = annotationService.getAnnotationList(Long.valueOf(user.getId()), resourceId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.fail("批注查询失败：" + e.getMessage());
        }
    }
}
