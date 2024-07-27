package com.lzlz.springboot.security.jwt;

import org.springframework.security.core.AuthenticationException;

public class InvalidJwtAuthenticationException extends AuthenticationException {
    private static final long serialVersionUID = -761503632186596342L;

    /**
     * 表示JWT token无效或过期的异常
 * @param e
     * @return
     * @author liuzheng
     * @create 2024-07-27
     **/
    public InvalidJwtAuthenticationException(String e) {
        super(e);
    }
}