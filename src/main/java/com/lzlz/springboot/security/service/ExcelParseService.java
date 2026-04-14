package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.entity.StudentExcelDTO;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelParseService {

    public List<StudentExcelDTO> parse(MultipartFile file) throws Exception {
        List<StudentExcelDTO> dtoList = new ArrayList<>();

        // 使用 try-with-resources 自动释放文件流和 Workbook 内存
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            // 获取第一个 Sheet 页
            Sheet sheet = workbook.getSheetAt(0);

            // sheet.getLastRowNum() 返回最后一行数据的索引（基于 0）
            // i = 1 代表跳过表头（第 0 行: 序号,班级,姓名,用户名,初始密码）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                // 如果是空行，直接跳过
                if (row == null) {
                    continue;
                }

                StudentExcelDTO dto = new StudentExcelDTO();

                // ⚠️ 根据你的表格列顺序：
                // 列 0: 序号 (暂不需要，跳过读取)
                // 列 1: 班级
                // 列 2: 姓名
                // 列 3: 用户名
                // 列 4: 初始密码

                // --- 1. 读取【班级】 ---
                Cell classCell = row.getCell(1);
                if (classCell != null) {
                    classCell.setCellType(CellType.STRING); // 强制当作字符串读取
                    dto.setClassName(classCell.getStringCellValue().trim());
                }

                // --- 2. 读取【姓名】 ---
                Cell nameCell = row.getCell(2);
                if (nameCell != null) {
                    nameCell.setCellType(CellType.STRING);
                    dto.setStudentName(nameCell.getStringCellValue().trim());
                }

                // --- 3. 读取【用户名】 ---
                Cell usernameCell = row.getCell(3);
                if (usernameCell != null) {
                    usernameCell.setCellType(CellType.STRING);
                    dto.setUsername(usernameCell.getStringCellValue().trim());
                }

                // --- 4. 读取【初始密码】 ---
                Cell pwdCell = row.getCell(4);
                if (pwdCell != null) {
                    pwdCell.setCellType(CellType.STRING);
                    dto.setPassword(pwdCell.getStringCellValue().trim());
                }

                // 简单的防脏数据校验：如果用户名为空，说明这行可能是用户多点出来的无效空行，跳过不录入
                if (!StringUtils.hasText(dto.getUsername())) {
                    continue;
                }

                // 将该行数据加入集合
                dtoList.add(dto);
            }
        }

        for (StudentExcelDTO dto : dtoList) {
            String username = dto.getUsername();
            String password = dto.getPassword();
            
            // Excel中没有这两个字段，设定默认值（或者如果需要，你可以从Excel补充读取）
            String role = "ROLE_USER"; 
            String email = ""; 

            // 检查用户是否存在
            if (userDetailsService.userExists(username)) {
                dto.setSuccess(false);
                dto.setMessage("注册失败：用户名已存在");
                continue; // 跳过当前用户，继续注册下一个
            }

            try {
                // 创建新用户并保存
                User newUser = new User(username, password, role, email);
                userDetailsService.createUser(newUser);

                // 注册成功，回填数据
                dto.setUserId(newUser.getId()); // 假设 newUser 存在 getId() 方法
                dto.setSuccess(true);
                dto.setMessage("注册成功");
            } catch (Exception e) {
                // 捕获数据库约束或其他异常，防止阻断整个批量流程
                dto.setSuccess(false);
                dto.setMessage("注册失败：系统异常 (" + e.getMessage() + ")");
            }
        }
        
        if (dtoList.isEmpty()) {
            throw new RuntimeException("未能从文件中解析出有效数据，请确保已填写数据且未使用空白模板。");
        }

        return dtoList;
    }
}
