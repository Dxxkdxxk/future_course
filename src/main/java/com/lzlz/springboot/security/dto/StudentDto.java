package com.lzlz.springboot.security.dto;

import lombok.Data;

public class StudentDto {

    /**
     * 手动添加学生请求参数
     */
    @Data
    public static class CreateRequest {
        // courseId 从 URL 获取
        private String name;
        private String studentNumber;
        private String gender; // 可选: "男", "女"
    }

    /**
     * 学生信息响应
     */
    @Data
    public static class Response {
        private Long id;
        private String name;
        private String studentNumber;
        private String gender;
    }
}