package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzlz.springboot.security.dto.QuestionDto;
import com.lzlz.springboot.security.entity.Question;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.mapper.QuestionMapper;
import com.lzlz.springboot.security.service.QuestionService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Override
    public String createQuestion(Long courseId, QuestionDto.CreateRequest request) {
        Question question = new Question();
        question.setCourseId(courseId);
        question.setStem(request.getStem());
        question.setType(request.getType());
        question.setTopic(request.getTopic());
        question.setDifficulty(request.getDifficulty());
        question.setScore(request.getScore());
        question.setEstimatedTime(request.getEstimatedTime());

        // 保存新增字段
        question.setAnswer(request.getAnswer());
        question.setAnalysis(request.getAnalysis());

        this.save(question);
        return question.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importQuestions(Long courseId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        List<Question> questions = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) { // .xlsx 格式

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                // 跳过表头 (第1行)
                if (row.getRowNum() < 1) continue;

                // 跳过第一列为空的行 (防止读取空行)
                if (row.getCell(0) == null) continue;

                Question q = new Question();
                q.setCourseId(courseId);

                // --- 读取字符串列 ---
                // 0: 题干, 1: 类型, 2: 知识点, 3: 难度
                q.setStem(getCellStringValue(row.getCell(0)));
                q.setType(getCellStringValue(row.getCell(1)));
                q.setTopic(getCellStringValue(row.getCell(2)));
                q.setDifficulty(getCellStringValue(row.getCell(3)));

                // --- 读取数字列 (使用修复后的方法) ---
                // 4: 分值, 5: 时长
                // 即使Excel里是 "10" (文本格式)，getCellIntValue 也能正确解析
                q.setScore(getCellIntValue(row.getCell(4)));
                q.setEstimatedTime(getCellIntValue(row.getCell(5)));

                // --- 读取新增字段 ---
                // 6: 答案, 7: 解析
                q.setAnswer(getCellStringValue(row.getCell(6)));
                q.setAnalysis(getCellStringValue(row.getCell(7)));

                questions.add(q);
            }

            if (!questions.isEmpty()) {
                this.saveBatch(questions);
            }

            return questions.size();

        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomGraphException(400, "Excel 解析失败: " + e.getMessage());
        }
    }

    @Override
    public List<Question> getQuestions(Long courseId, QuestionDto.QueryRequest query) {
        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);

        if (query.getType() != null && !query.getType().isEmpty()) {
            wrapper.eq("type", query.getType());
        }
        if (query.getDifficulty() != null && !query.getDifficulty().isEmpty()) {
            wrapper.eq("difficulty", query.getDifficulty());
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like("stem", query.getKeyword())
                    .or()
                    .like("topic", query.getKeyword()));
        }
        // 支持按ID筛选
        if (query.getQuestions() != null && !query.getQuestions().isEmpty()) {
            String[] ids = query.getQuestions().split(",");
            wrapper.in("id", (Object[]) ids);
        }

        wrapper.orderByDesc("created_at");
        return this.list(wrapper);
    }

    /**
     * (!!!) 核心修复方法：兼容数字格式和文本格式的数字读取
     */
    private Integer getCellIntValue(Cell cell) {
        if (cell == null) {
            return 0;
        }
        // 情况1: Excel 单元格格式是 "数值"
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        // 情况2: Excel 单元格格式是 "文本" (例如左上角有绿色小三角)
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) return 0;
            try {
                // 尝试将字符串 "10" 解析为整数 10
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                // 如果解析失败(例如填了 "10分")，返回0或抛出异常
                System.err.println("解析数字失败: " + val);
                return 0;
            }
        }
        return 0;
    }

    /**
     * 辅助方法：强制以字符串格式读取单元格
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        // 强制设置类型为 String，防止读取数字时报错
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}