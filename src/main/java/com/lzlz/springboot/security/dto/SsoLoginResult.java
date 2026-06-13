package com.lzlz.springboot.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoLoginResult {
    private Integer userId;
    private String username;
    private String name;
    private String employeeNumber;
    private String role;
    private String token;
}
