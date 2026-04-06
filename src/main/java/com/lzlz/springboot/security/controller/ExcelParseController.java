package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.entity.StudentExcelDTO;
import com.lzlz.springboot.security.response.Result;
import com.lzlz.springboot.security.service.ExcelParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/excel")
public class ExcelParseController {

    @Autowired
    private ExcelParseService excelParseService;

    /**
     * 解析学生名单文件并回传给前端
     */
    @PostMapping("/parse-students")
    public Result<List<StudentExcelDTO>> parseStudentExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.fail("上传的文件不能为空");
            }

            // 调用 Service 层解析文件
            List<StudentExcelDTO> parsedList = excelParseService.parse(file);

            // 直接将解析得到的数据返回给前端
            return Result.success(parsedList);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("文件解析失败: " + e.getMessage());
        }
    }
}
