package com.lzlz.springboot.security.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentExcelDTO {

    private String className;

    private String studentName;

    private String username;

    private String password;
}
