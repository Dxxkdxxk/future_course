package com.lzlz.springboot.security.exception;

/**
 * 自定义业务异常
 * 用于携带 30201, 30202, 30203 等错误码
 */
public class CustomGraphException extends RuntimeException {
    private final int code;

    public CustomGraphException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}