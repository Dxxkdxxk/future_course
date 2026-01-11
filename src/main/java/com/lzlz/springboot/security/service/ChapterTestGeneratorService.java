package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.GenerateTestDto;
import com.lzlz.springboot.security.entity.*;
import com.lzlz.springboot.security.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChapterTestGeneratorService {

    @Autowired private PaperMapper paperMapper;
    @Autowired private QuestionMapper questionMapper;
    @Autowired private PaperQuestionMapper paperQuestionMapper;
    @Autowired private ChapterPaperRefMapper chapterPaperRefMapper;

    /**
     * (!!!) 修复点：返回类型从 void 改为 Map<String, Long>
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> generateTest(String courseIdStr, String chapterIdStr, GenerateTestDto.Request request) {

        Long courseId = Long.parseLong(courseIdStr);
        Long chapterId = Long.parseLong(chapterIdStr);

        // 1. 创建试卷
        Paper paper = new Paper();
        paper.setCourseId(courseId);
        paper.setTitle(request.getTestName());
        paper.setDuration(request.getTestDuration());
        paper.setTotalScore(request.getTotalScore());
        paper.setDescription(request.getSection());
        paper.setStatus(1);
        // (!!!) 新增这行代码：修复逻辑隐患
// 如果 totalScore 不为空，则设置及格分为 60%，否则设为 0
        if (request.getTotalScore() != null) {paper.setPassScore((int) (request.getTotalScore() * 0.6));}
        else {
            paper.setPassScore(0);
        }
        paperMapper.insert(paper);

        // 2. 保存题目并关联
        List<GenerateTestDto.SelectedQuestionItem> items = request.getSelectedQuestions();
        if (items != null && !items.isEmpty()) {
            int sortOrder = 1;
            for (GenerateTestDto.SelectedQuestionItem item : items) {
                // 保存新题目
                Question q = new Question();
                q.setCourseId(courseId);
                q.setStem(item.getStem());
                q.setType(item.getType());
                q.setDifficulty(item.getDifficulty());
                q.setScore(item.getScore());
                q.setTopic(request.getSection());
                q.setAnswer(item.getAnswer());
                q.setAnalysis(item.getAnalysis());
                questionMapper.insert(q);

                // 关联题目到试卷
                PaperQuestion pq = new PaperQuestion();
                pq.setPaperId(paper.getId());
                pq.setQuestionId(q.getId());
                pq.setScore(item.getScore());
                pq.setSortOrder(sortOrder++);
                paperQuestionMapper.insert(pq);
            }
        }

        // 3. 绑定章节与试卷
        QueryWrapper<ChapterPaperRef> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("chapter_id", chapterId);
        chapterPaperRefMapper.delete(deleteWrapper);

        ChapterPaperRef ref = new ChapterPaperRef();
        ref.setChapterId(chapterId);
        ref.setPaperId(paper.getId());
        chapterPaperRefMapper.insert(ref);

        // (!!!) 修复点：构造并返回 Map
        Map<String, Long> result = new HashMap<>();
        result.put("paperId", paper.getId());
        result.put("chapterId", chapterId);
        return result;
    }

    /**
     * 获取测试详情 (保持不变)
     */
    public GenerateTestDto.Request getTestDetails(String chapterIdStr) {
        Long chapterId = Long.parseLong(chapterIdStr);

        QueryWrapper<ChapterPaperRef> refWrapper = new QueryWrapper<>();
        refWrapper.eq("chapter_id", chapterId);
        ChapterPaperRef ref = chapterPaperRefMapper.selectOne(refWrapper);

        if (ref == null) return null;

        Paper paper = paperMapper.selectById(ref.getPaperId());
        if (paper == null) return null;

        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", paper.getId());
        pqWrapper.orderByAsc("sort_order");
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        List<GenerateTestDto.SelectedQuestionItem> questionItems = new ArrayList<>();
        if (!paperQuestions.isEmpty()) {
            List<String> qIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
            List<Question> questions = questionMapper.selectBatchIds(qIds);
            Map<String, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));

            for (PaperQuestion pq : paperQuestions) {
                Question q = qMap.get(pq.getQuestionId());
                if (q != null) {
                    GenerateTestDto.SelectedQuestionItem item = new GenerateTestDto.SelectedQuestionItem();
                    item.setStem(q.getStem());
                    item.setType(q.getType());
                    item.setDifficulty(q.getDifficulty());
                    item.setScore(pq.getScore() != null ? pq.getScore() : q.getScore());
                    questionItems.add(item);
                }
            }
        }

        GenerateTestDto.Request response = new GenerateTestDto.Request();
        response.setSection(paper.getDescription());
        response.setTestName(paper.getTitle());
        response.setTestDuration(paper.getDuration());
        response.setTotalScore(paper.getTotalScore());
        response.setSelectedQuestions(questionItems);

        return response;
    }
}