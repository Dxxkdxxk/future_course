package com.lzlz.springboot.security.exception;

// 这是一个非受检异常，专门用于表示 404
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}