package com.lzlz.springboot.security.response;

import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import javax.validation.constraints.NotNull;

@Data
public class AnnotationAddRequest {
    @NotNull(message = "教材ID不能为空")
    private Long resourceId;

    @NotNull()
    private int startOffset;

    @NotNull()
    private int endOffset;

    private String selectedText;

    @NotNull()
    private String highlightColor;

    private String annotationType;

    private String comment;
    private String minioObjectName;
}
