package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class ApiResponse<T> {

    private int code;
    private String msg;
    private T data;


    public ApiResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功的响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    /**
     * 成功的响应（不带数据，例如删除）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    /**
     * 失败的响应（使用自定义错误码和消息）
     */
    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

    /**
     * 创建成功的响应 (HTTP 201)
     *
     @param
     data 创建的资源
     */
    public static <T> ApiResponse<T> created(T data)
    {
        // (!!) 你的示例中 code 为 0, 但标准HTTP 201 更清晰
        // (!!) 我们这里使用 201，并修改消息
        return new ApiResponse<>(201, "创建成功", data);
    }
}