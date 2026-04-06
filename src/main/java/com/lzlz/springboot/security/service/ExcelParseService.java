package com.lzlz.springboot.security.service;

import com.alibaba.excel.EasyExcel;
import com.lzlz.springboot.security.entity.StudentExcelDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ExcelParseService {

    public List<StudentExcelDTO> parse(MultipartFile file) throws IOException {

        // 使用 EasyExcel 同步读取所有数据到内存中
        // .sheet() 默认读取第一个 Sheet
        // .doReadSync() 会返回一个 List<StudentExcelDTO>
        List<StudentExcelDTO> dtoList = EasyExcel.read(file.getInputStream())
                .head(StudentExcelDTO.class)
                .sheet()
                .doReadSync();

        if (dtoList == null || dtoList.isEmpty()) {
            throw new RuntimeException("未能从文件中解析出任何数据，请检查文件内容或表头是否匹配");
        }

        return dtoList;
    }
}
