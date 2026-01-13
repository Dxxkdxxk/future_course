package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.ExamDto;
import com.lzlz.springboot.security.entity.*;
import com.lzlz.springboot.security.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExamService {

    @Autowired private PaperMapper paperMapper;
    @Autowired private PaperQuestionMapper paperQuestionMapper;
    @Autowired private QuestionMapper questionMapper;          // 题库
    @Autowired private ChapterPaperRefMapper chapterRefMapper; // 章节关联
    @Autowired private StudentPaperRecordMapper recordMapper;  // 考试记录
    @Autowired private StudentPaperDetailMapper detailMapper;  // 答题详情

    // ==========================================
    // 1. [教师] 组卷：从现有题库挑选题目
    // ==========================================
    @Transactional(rollbackFor = Exception.class)
    public Long createPaper(Long courseId, ExamDto.CreatePaperRequest request) {
        // A. 校验题目有效性并计算总分
        List<Question> selectedQuestions = questionMapper.selectBatchIds(request.getQuestionIds());
        if (selectedQuestions.size() != request.getQuestionIds().size()) {
            throw new RuntimeException("部分题目ID不存在，请刷新题库");
        }

        int totalScore = selectedQuestions.stream().mapToInt(Question::getScore).sum();

        // B. 保存试卷主表
        Paper paper = new Paper();
        paper.setCourseId(courseId);
        paper.setTitle(request.getTitle());
        paper.setDescription(request.getDescription());
        paper.setDuration(request.getDuration());
        paper.setTotalScore(totalScore);
        paper.setPassScore((int) (totalScore * 0.6)); // 默认60%及格
        paper.setStatus(1); // 已发布
        paperMapper.insert(paper);

        // C. 保存试卷-题目关联
        // 保持前端传入的顺序
        for (int i = 0; i < request.getQuestionIds().size(); i++) {
            String qId = request.getQuestionIds().get(i);
            // 找到对应的原题获取分数
            Question originalQ = selectedQuestions.stream().filter(q -> q.getId().equals(qId)).findFirst().orElse(null);

            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(qId);
            pq.setSortOrder(i + 1);
            pq.setScore(originalQ != null ? originalQ.getScore() : 0); // 使用原题分数
            paperQuestionMapper.insert(pq);
        }

        return paper.getId();
    }

    // ==========================================
    // 2. [教师] 发布：将试卷挂载到章节
    // ==========================================
    @Transactional(rollbackFor = Exception.class)
    public void publishToChapter(Long chapterId, Long paperId) {
        // 先检查试卷是否存在
        if (paperMapper.selectById(paperId) == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 清理该章节旧的试卷关联 (假设一章一卷)
        QueryWrapper<ChapterPaperRef> wrapper = new QueryWrapper<>();
        wrapper.eq("chapter_id", chapterId);
        chapterRefMapper.delete(wrapper);

        // 创建新关联
        ChapterPaperRef ref = new ChapterPaperRef();
        ref.setChapterId(chapterId);
        ref.setPaperId(paperId);
        chapterRefMapper.insert(ref);
    }

    // ==========================================
    // 3. [学生] 参加考试：获取试卷 (无答案)
    // ==========================================
    public ExamDto.PaperView getPaperForStudent(Long chapterId) {
        // A. 查关联
        QueryWrapper<ChapterPaperRef> refWrapper = new QueryWrapper<>();
        refWrapper.eq("chapter_id", chapterId);
        ChapterPaperRef ref = chapterRefMapper.selectOne(refWrapper);

        if (ref == null) throw new RuntimeException("该章节暂无测试");

        // B. 查试卷
        Paper paper = paperMapper.selectById(ref.getPaperId());

        // C. 查题目列表
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", paper.getId()).orderByAsc("sort_order");
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(pqWrapper);

        List<String> qIds = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<String, Question> qMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // D. 组装视图 (脱敏)
        List<ExamDto.QuestionView> qViews = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = qMap.get(pq.getQuestionId());
            if (q != null) {
                ExamDto.QuestionView v = new ExamDto.QuestionView();
                v.setId(q.getId());
                v.setStem(q.getStem());
                v.setType(q.getType());
                v.setDifficulty(q.getDifficulty());
                v.setScore(pq.getScore()); // 使用试卷设定的分数
                qViews.add(v);
            }
        }

        ExamDto.PaperView view = new ExamDto.PaperView();
        view.setPaperId(paper.getId());
        view.setTitle(paper.getTitle());
        view.setDuration(paper.getDuration());
        view.setTotalScore(paper.getTotalScore());
        view.setQuestions(qViews);

        return view;
    }

    // ==========================================
    // 4. [学生] 交卷：自动判分
    // ==========================================
    @Transactional(rollbackFor = Exception.class)
    public ExamDto.ResultView submitExam(int studentId, Long chapterId, ExamDto.SubmitRequest request) {
        // A. 重新获取试卷信息以进行比对
        QueryWrapper<ChapterPaperRef> refWrapper = new QueryWrapper<>();
        refWrapper.eq("chapter_id", chapterId);
        ChapterPaperRef ref = chapterRefMapper.selectOne(refWrapper);
        if (ref == null) throw new RuntimeException("考试已失效");

        Paper paper = paperMapper.selectById(ref.getPaperId());

        // 获取标准答案
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", paper.getId());
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(pqWrapper);

        List<String> qIds = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<String, Question> standardMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<String, Integer> scoreMap = pqs.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore));

        // B. 判分逻辑
        int myScore = 0;
        int correctCount = 0;
        List<StudentPaperDetail> details = new ArrayList<>();

        for (ExamDto.AnswerItem item : request.getAnswers()) {
            Question stdQ = standardMap.get(item.getQuestionId());
            if (stdQ == null) continue;

            boolean isCorrect = false;
            // 简单字符串比对 (忽略大小写和首尾空格)
            String stdAns = stdQ.getAnswer() == null ? "" : stdQ.getAnswer().trim();
            String myAns = item.getMyAnswer() == null ? "" : item.getMyAnswer().trim();

            if (stdAns.equalsIgnoreCase(myAns)) {
                isCorrect = true;
                myScore += scoreMap.getOrDefault(item.getQuestionId(), 0);
                correctCount++;
            }

            StudentPaperDetail detail = new StudentPaperDetail();
            detail.setQuestionId(item.getQuestionId());
            detail.setStudentAnswer(item.getMyAnswer());
            detail.setIsCorrect(isCorrect);
            detail.setScoreGained(isCorrect ? scoreMap.get(item.getQuestionId()) : 0);
            details.add(detail);
        }

        // C. 保存记录
        StudentPaperRecord record = new StudentPaperRecord();
        record.setStudentId(studentId);
        record.setPaperId(paper.getId());
        record.setChapterId(chapterId);
        record.setTotalScore(myScore);
        record.setFullScore(paper.getTotalScore());
        record.setIsPassed(myScore >= paper.getPassScore());
        recordMapper.insert(record);

        for (StudentPaperDetail d : details) {
            d.setRecordId(record.getId());
            detailMapper.insert(d);
        }

        // D. 返回结果
        ExamDto.ResultView result = new ExamDto.ResultView();
        result.setRecordId(record.getId());
        result.setMyScore(myScore);
        result.setTotalScore(paper.getTotalScore());
        result.setIsPassed(record.getIsPassed());
        result.setCorrectCount(correctCount);

        return result;
    }
}