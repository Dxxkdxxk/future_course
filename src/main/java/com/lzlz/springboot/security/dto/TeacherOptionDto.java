package com.lzlz.springboot.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeacherOptionDto {
    private Integer id;
    private String username;
    private String role;
}
