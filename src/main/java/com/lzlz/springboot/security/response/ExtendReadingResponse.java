package com.lzlz.springboot.security.response;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExtendReadingResponse {
    private Long id;
    private String materialType;
    private String title;
    private JSONObject positionInfo;
    private String url;
    private String content;
    private String fileUrl;
    private LocalDateTime createTime;
}
