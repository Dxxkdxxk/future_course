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

        // (!!!) 保存答案和解析
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

            // 从第2行开始读取 (跳过表头)
            for (Row row : sheet) {
                if (row.getRowNum() < 1) continue;
                if (row.getCell(0) == null) continue; // 跳过空行

                Question q = new Question();
                q.setCourseId(courseId);

                // 读取 Excel 列 (索引从 0 开始)
                // 0: 题干, 1: 类型, 2: 知识点, 3: 难度
                q.setStem(getCellStringValue(row.getCell(0)));
                q.setType(getCellStringValue(row.getCell(1)));
                q.setTopic(getCellStringValue(row.getCell(2)));
                q.setDifficulty(getCellStringValue(row.getCell(3)));

                // 4: 分数, 5: 时长 (数字类型)
                // 注意: getNumericCellValue 返回 double，需要强转 int
                if (row.getCell(4) != null) {
                    q.setScore((int) row.getCell(4).getNumericCellValue());
                } else {
                    q.setScore(0);
                }

                if (row.getCell(5) != null) {
                    q.setEstimatedTime((int) row.getCell(5).getNumericCellValue());
                } else {
                    q.setEstimatedTime(0);
                }

                // (!!!) 6: 答案, 7: 解析
                q.setAnswer(getCellStringValue(row.getCell(6)));
                q.setAnalysis(getCellStringValue(row.getCell(7)));

                questions.add(q);
            }

            if (!questions.isEmpty()) {
                this.saveBatch(questions);
            }

            return questions.size();

        } catch (Exception e) {
            e.printStackTrace(); // 方便调试
            throw new CustomGraphException(400, "Excel 解析失败: " + e.getMessage() + "。请确保上传的是 .xlsx 文件且格式正确。");
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
        if (query.getQuestions() != null && !query.getQuestions().isEmpty()) {
            String[] ids = query.getQuestions().split(",");
            wrapper.in("id", (Object[]) ids);
        }

        wrapper.orderByDesc("created_at");
        return this.list(wrapper);
    }

    // 辅助方法：安全获取字符串
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        // 强制设为 String 类型读取，避免数字读取成 "10.0" 这种格式
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }
}