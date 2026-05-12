package com.lzlz.springboot.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class SaveVideoProgressDTO {

    @NotBlank(message = "资源ID不能为空")
    private String resourceId;  // 对应前端：resourceId（字符串）

    @NotNull(message = "已观看时长不能为空")
    private Integer watchedSeconds;  // 对应前端：watchedSeconds

    @NotNull(message = "视频总时长不能为空")
    private Integer durationSeconds; // 对应前端：durationSeconds
}
