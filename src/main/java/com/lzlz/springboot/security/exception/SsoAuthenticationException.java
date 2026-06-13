package com.lzlz.springboot.security.exception;

public class SsoAuthenticationException extends RuntimeException {
    public SsoAuthenticationException(String message) {
        super(message);
    }

    public SsoAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
