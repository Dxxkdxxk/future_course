package com.lzlz.springboot.security.controller;


import com.lzlz.springboot.security.response.ExtendReadingAddRequest;
import com.lzlz.springboot.security.response.ExtendReadingResponse;
import com.lzlz.springboot.security.response.Result;
import com.lzlz.springboot.security.service.ExtendReadingService;
import com.lzlz.springboot.security.service.MinIOService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/extend-reading")
public class ExtendReadingController {

    @Autowired
    private ExtendReadingService extendReadingService;

    @Resource
    private MinIOService minIOService;

    /**
     * 添加拓展阅读材料
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Long>> addExtendReading(@Validated @RequestBody ExtendReadingAddRequest request,
    @RequestPart(value = "uploadFile", required = false) MultipartFile uploadFile
    ) {
        try {

            if (uploadFile != null && !uploadFile.isEmpty()) {
                String objectName = minIOService.uploadFile(uploadFile);
                // 给request填充MinIO相关字段（后续存入数据库）
                request.setMinioBucket(minIOService.getMinioBucket());
                request.setMinioObjectName(objectName);
                request.setFileSize(uploadFile.getSize());
                request.setFileType(uploadFile.getContentType());
            }
            Long readingId = extendReadingService.addExtendReading(request);
            Map<String, Long> data = new HashMap<>();
            data.put("readingId", readingId);
            return Result.success("拓展阅读添加成功", data);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            return Result.fail("拓展阅读添加失败：" + e.getMessage());
        }
    }

    /**
     * 查询拓展阅读列表
     */
    @GetMapping
    public Result<List<ExtendReadingResponse>> getExtendReadings(
            @RequestParam String studentId,
            @RequestParam Long textbookId,
            @RequestParam(required = false) Long chapterId) {
        try {
            List<ExtendReadingResponse> list = extendReadingService.getExtendReadingList(studentId, textbookId, chapterId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.fail("拓展阅读查询失败：" + e.getMessage());
        }
    }

    /**
     * 删除拓展阅读（仅本人可删）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteExtendReading(
            @PathVariable Long id,
            @RequestParam String studentId) {
        try {
            boolean success = extendReadingService.deleteExtendReading(id, studentId);
            if (success) {
                return Result.success();
            } else {
                return Result.fail(400, "拓展阅读删除失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            return Result.fail("拓展阅读删除失败：" + e.getMessage());
        }
    }
}
