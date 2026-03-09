package com.lzlz.springboot.security.response;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnotationResponse {
    private Long id;

    private Long studentId;

    /**
     * 教材ID（对应前端resourceId）
     */
    private Long resourceId;

    /**
     * 起始偏移量
     */
    private Integer startOffset;

    /**
     * 结束偏移量
     */
    private Integer endOffset;

    /**
     * 选中的文本
     */
    private String selectedText;

    /**
     * 高亮颜色
     */
    private String highlightColor;

    /**
     * 标注类型（highlight/note）
     */
    private String annotationType;

    /**
     * 备注
     */
    private String comment;

    private String fileUrl;
    /**
     * 创建时间（数据库自动生成，前端无需传）
     */
    private LocalDateTime createTime;
}
