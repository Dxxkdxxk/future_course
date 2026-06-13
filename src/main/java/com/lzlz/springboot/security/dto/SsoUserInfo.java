package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SsoUserInfo {
    private String username;
    private String name;
    private String employeeNumber;
    private Map<String, String> attributes = new HashMap<>();

    public String getLocalUsername() {
        if (employeeNumber != null && !employeeNumber.isBlank()) {
            return employeeNumber.trim();
        }
        return username == null ? null : username.trim();
    }
}
