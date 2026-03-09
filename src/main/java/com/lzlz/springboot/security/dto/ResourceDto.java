package com.lzlz.springboot.security.dto;

import lombok.Data;
// (!!!) 记得导入 json 序列化包，防止前端 JS 丢失 Long 精度
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class ResourceDto {

    @Data
    public static class UploadRequest {
        // (!!!) 修改点：前端上传后返回的 MinIO ID 是 Long
        private Long resourceId;

        private String materialName;
        private String materialType;
        private String section;
        private Boolean isRequired;
        private Boolean isVedio;
        private String materialDescription;
        private String fileSize;
    }

    @Data
    public static class ResourceView {
        // (!!!) 修改点：返回给前端的也是 Long
        // 建议加上 @JsonSerialize(using = ToStringSerializer.class)
        // 防止前端 JavaScript 处理 19 位 Long 类型时精度丢失
        @JsonSerialize(using = ToStringSerializer.class)
        private String resourceName;
        private String materialType;

        // 4. MinIO 文件 ID (用于下载/预览)
        private Long resourceId;

        private String fileSize;
        private Boolean isVideo;
        private Boolean isRequired;

        // 5. 更多描述信息
        private String section;
        private String description;
    }
}