package com.lzlz.springboot.security.entity;

import lombok.Data;

@Data
public class StudentExcelDTO {

    private String className;

    private String studentName;

    private String username;

    private String password;

    private Integer userId;       // 注册成功后返回的ID (如果是String类型请自行修改)
    private boolean success;   // 标识该条记录是否注册成功
    private String message;    // 存储成功或失败的提示信息
}
