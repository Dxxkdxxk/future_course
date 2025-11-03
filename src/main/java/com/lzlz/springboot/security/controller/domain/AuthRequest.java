package com.lzlz.springboot.security.controller.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;

    private String role;
    private String email;
    public AuthRequest() {
    }

    public AuthRequest(String username, String password, String role, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
