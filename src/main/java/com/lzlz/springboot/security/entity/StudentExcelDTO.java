package com.lzlz.springboot.security.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentExcelDTO {

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("姓名")
    private String studentName;

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("初始密码")
    private String password;
}
