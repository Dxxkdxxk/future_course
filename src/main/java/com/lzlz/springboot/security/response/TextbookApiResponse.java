package com.lzlz.springboot.security.response;

import lombok.Data;

@Data
public class TextbookApiResponse<T> {
    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应数据
     */
    private T data;

    // 快捷构造方法
    public static <T> TextbookApiResponse<T> success(T data) {
        TextbookApiResponse<T> response = new TextbookApiResponse<>();
        response.setCode(200);
        response.setData(data);
        return response;
    }
}
