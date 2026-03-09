package com.lzlz.springboot.security.response;

import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import javax.validation.constraints.NotNull;

@Data
public class ExtendReadingAddRequest {
    @NotBlank(message = "学生ID不能为空")
    private String studentId;

    @NotNull(message = "教材ID不能为空")
    private Long textbookId;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    @NotNull(message = "位置信息不能为空")
    private JSONObject positionInfo;

    @NotBlank(message = "材料类型不能为空")
    private String materialType;

    @NotBlank(message = "标题不能为空")
    private String title;

    // URL类型字段
    private String url;

    // TEXT类型字段
    private String content;

    // FILE类型字段
    private String minioBucket;
    private String minioObjectName;
    private Long fileSize;
    private String fileType;
}
