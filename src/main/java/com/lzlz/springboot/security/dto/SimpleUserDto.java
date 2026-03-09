package com.lzlz.springboot.security.dto;

import lombok.Data;

@Data
public class SimpleUserDto {
    private Integer id;
    private String username;
    private String email;
    private String role;

    // 如果需要，可以加个构造函数方便转换
    public SimpleUserDto(Integer id, String username, String email, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}