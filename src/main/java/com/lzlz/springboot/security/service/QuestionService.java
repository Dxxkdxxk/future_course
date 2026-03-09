package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.dto.QuestionDto;
import com.lzlz.springboot.security.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface QuestionService extends IService<Question> {

    // 创建题目
    String createQuestion(Long courseId, QuestionDto.CreateRequest request);

    // 导入题目 (Excel)
    int importQuestions(Long courseId, MultipartFile file);

    // 获取题目列表
    List<Question> getQuestions(Long courseId, QuestionDto.QueryRequest query);
}