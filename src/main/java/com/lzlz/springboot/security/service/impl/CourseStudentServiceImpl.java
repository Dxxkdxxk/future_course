package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzlz.springboot.security.dto.StudentDto;
import com.lzlz.springboot.security.entity.CourseStudent;
import com.lzlz.springboot.security.exception.CustomGraphException; // 复用已有异常
import com.lzlz.springboot.security.mapper.CourseStudentMapper;
import com.lzlz.springboot.security.service.CourseStudentService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class CourseStudentServiceImpl extends ServiceImpl<CourseStudentMapper, CourseStudent> implements CourseStudentService {

    @Override
    public void addStudent(Long courseId, StudentDto.CreateRequest request) {
        // 1. 简单的查重校验 (防止同一课程添加相同学号)
        if (checkStudentExists(courseId, request.getStudentNumber())) {
            throw new CustomGraphException(400, "该学号已存在于本课程中: " + request.getStudentNumber());
        }

        CourseStudent student = new CourseStudent();
        student.setCourseId(courseId);
        student.setName(request.getName());
        student.setStudentNumber(request.getStudentNumber());
        student.setGender(request.getGender());

        this.save(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importStudents(Long courseId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        List<CourseStudent> students = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) { // .xlsx

            Sheet sheet = workbook.getSheetAt(0);

            // 从第2行开始读取 (跳过表头)
            for (Row row : sheet) {
                if (row.getRowNum() < 1) continue;
                // 如果第一列(姓名)为空，则跳过
                if (row.getCell(0) == null) continue;

                String name = getCellStringValue(row.getCell(0));
                String studentNumber = getCellStringValue(row.getCell(1));
                String gender = getCellStringValue(row.getCell(2));

                // 简单的校验
                if (name.isBlank() || studentNumber.isBlank()) {
                    continue;
                }

                // 内存中去重校验 (可选，如果Excel里有重复)
                // 数据库层面也有唯一索引保护
                if (checkStudentExists(courseId, studentNumber)) {
                    // 策略：跳过已存在的，或者抛出异常。这里选择跳过。
                    continue;
                }

                CourseStudent student = new CourseStudent();
                student.setCourseId(courseId);
                student.setName(name);
                student.setStudentNumber(studentNumber);
                student.setGender(gender);

                students.add(student);
            }

            if (!students.isEmpty()) {
                this.saveBatch(students);
            }

            return students.size();

        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomGraphException(400, "Excel 解析失败: " + e.getMessage());
        }
    }

    @Override
    public List<CourseStudent> getStudentsByCourse(Long courseId) {
        QueryWrapper<CourseStudent> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.orderByAsc("student_number"); // 按学号排序
        return this.list(wrapper);
    }

    private boolean checkStudentExists(Long courseId, String studentNumber) {
        QueryWrapper<CourseStudent> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId)
                .eq("student_number", studentNumber);
        return this.count(wrapper) > 0;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING); // 强制转为字符串，防止学号被读成数字(如 2.02101E8)
        return cell.getStringCellValue().trim();
    }
}