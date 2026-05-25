package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.assistant.RagAssistant;
import com.lzlz.springboot.security.assistant.StreamingRagAssistant;
import com.lzlz.springboot.security.dto.AiGradeSubmissionRequest;
import com.lzlz.springboot.security.rag.RagRetrievalContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 原 Dify 工作流能力：用 LangChain4j + 统一 Chat 模型（如 DeepSeek）在本地实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatLanguageModel chatLanguageModel;
    private final RagDocumentService documentService;
    private final RagAssistant ragAssistant;
    private final StreamingRagAssistant streamingRagAssistant;
    private final RagRetrievalContext ragRetrievalContext;

    private <T> T withRetrievalCourseId(String courseId, Supplier<T> action) {
        try {
            ragRetrievalContext.setCourseId(courseId == null ? null : courseId.trim());
            return action.get();
        } finally {
            ragRetrievalContext.clear();
        }
    }

    private String chat(String systemPrompt, String userPrompt) {
        Response<AiMessage> response = chatLanguageModel.generate(
                List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userPrompt)
                )
        );
        return response.content().text();
    }

    public String markHomework(MultipartFile file, String courseId) throws Exception {
        String content = documentService.extractPlainText(file);
        String excerpt = content.length() > 800 ? content.substring(0, 800) + "…" : content;
        String user = """
                课程 ID（供参考）：%s

                【供向量检索摘要】请据此从知识库匹配与本作业相关的讲义、习题与评分要点：
                %s

                以下是学生提交的作业全文：
                ---
                %s
                ---
                """.formatted(courseId == null ? "" : courseId, excerpt, content);
        return withRetrievalCourseId(courseId, () -> ragAssistant.markHomework(user));
    }

    public String gradeHomeworkJson(String courseId, String homeworkTitle, String homeworkContent,
                                    Integer totalScore, String studentContent, String extraInstruction,
                                    List<AiGradeSubmissionRequest.ScoringPoint> scoringPoints) {
        String content = studentContent == null ? "" : studentContent;
        String excerpt = content.length() > 800 ? content.substring(0, 800) + "…" : content;
        String scoringPointsText = formatScoringPoints(scoringPoints);
        String user = """
                课程 ID：%s

                作业标题：
                %s

                作业要求：
                %s

                作业总分：%s

                教师补充批改要求：
                %s

                得分点/分项评价依据：
                %s

                【供向量检索摘要】请据此从知识库匹配与本作业相关的讲义、习题与评分要点：
                %s

                学生提交内容：
                ---
                %s
                ---
                """.formatted(
                courseId == null ? "" : courseId,
                homeworkTitle == null ? "" : homeworkTitle,
                homeworkContent == null ? "" : homeworkContent,
                totalScore == null ? 100 : totalScore,
                extraInstruction == null ? "" : extraInstruction,
                scoringPointsText,
                excerpt,
                content
        );
        return withRetrievalCourseId(courseId, () -> ragAssistant.gradeHomeworkJson(user));
    }

    private String formatScoringPoints(List<AiGradeSubmissionRequest.ScoringPoint> scoringPoints) {
        if (scoringPoints == null || scoringPoints.isEmpty()) {
            return "无";
        }
        List<String> descriptions = scoringPoints.stream()
                .map(AiGradeSubmissionRequest.ScoringPoint::getDescription)
                .filter(description -> description != null && !description.isBlank())
                .map(description -> "- " + description.trim())
                .toList();
        return descriptions.isEmpty() ? "无" : String.join("\n", descriptions);
    }

    public String markExperiment(MultipartFile file, String courseId) throws Exception {
        String content = documentService.extractPlainText(file);
        String excerpt = content.length() > 800 ? content.substring(0, 800) + "…" : content;
        String user = """
                课程 ID（供参考）：%s

                【供向量检索摘要】请据此从知识库匹配与本实验相关的说明、要求或范例：
                %s

                以下是学生的实验报告全文：
                ---
                %s
                ---
                """.formatted(courseId == null ? "" : courseId, excerpt, content);
        return withRetrievalCourseId(courseId, () -> ragAssistant.markExperiment(user));
    }

    /**
     * 返回应为 JSON 数组字符串，元素字段与旧 Dify 题库约定一致（经 RAG 结合知识库命题）。
     */
    public String generateQuestions(String query, String courseId) {
        String q = query == null ? "" : query;
        String user = """
                课程 ID（供参考）：%s

                教师命题要求（须满足）：
                %s
                """.formatted(courseId == null ? "" : courseId, q);
        return withRetrievalCourseId(courseId, () -> ragAssistant.generateQuestions(user));
    }

    public String generatePaperQuestionIds(String requirement, String courseId, String questionsJson) {
        return generatePaperQuestionIds(requirement, courseId, questionsJson, "[]");
    }

    public String generatePaperQuestionIds(String requirement, String courseId, String questionsJson, String previousQuestionsJson) {
        String system = """
                你是智能组卷助手。请根据教师本轮要求，从给定题库中选择合适题目。
                规则：
                1. 只能选择当前题库中已经存在的题目 id，不得编造 id。
                2. 当前课程完整题库 JSON 只是候选题库，不是当前试卷，不能直接把整个题库当作试卷返回。上一版试卷题目 ID 列表才是当前正在修改的试卷。
                3. 如果上一版试卷题目 ID 列表非空，必须严格在上一版试卷基础上修改，不能扩展到整个题库。例如上一版有 7 道题，要求“去掉一道选择题”，返回结果应为 6 道题。
                4. 如果没有提供上一版试卷题目 ID 列表，或列表为空，则按本轮要求从零组卷。
                5. 返回最终完整试卷的题目 ID 数组，而不是只返回新增或删除的题，也不是返回候选题库。
                6. 只返回 JSON 字符串数组，例如：[\"id1\",\"id2\"]。
                7. 不要返回 markdown，不要解释，不要返回其他字段。
                """;
        String user = """
                课程 ID：%s

                本轮教师要求：
                %s

                上一版试卷题目 ID 列表：
                %s

                当前课程完整题库 JSON：
                %s
                """.formatted(
                courseId == null ? "" : courseId,
                requirement == null ? "" : requirement,
                previousQuestionsJson == null ? "[]" : previousQuestionsJson,
                questionsJson == null ? "[]" : questionsJson
        );
        return chat(system, user);
    }

    public String outlineFromDocument(MultipartFile file, String courseId, String textbookId) throws Exception {
        String fileContent = documentService.extractPlainText(file);
        String system = "你是教材编辑助手，请用中文输出该教材的大纲。";
        String user = """
                请根据以下教材/章节内容，生成本章的大纲、章节概览与章节总结。

                %s
                """.formatted(
                fileContent);
        return chat(system, user);
    }

    public String richMediaRecommendations(String query, String courseId, String textbookId) {
        String system = "你是富媒体内容推荐助手，请用中文输出推荐的富媒体资源。";
        String user = """
                请理解用户描述，识别关键实体、场景或动作，推荐合适的富媒体资源（图片、视频、音频等）。
                链接须为真实可访问的公开资源；若无法确保真实，请说明并给出检索关键词而非编造 URL。

                输出时每条严格使用以下格式：
                [序号]
                推荐链接：
                推荐理由：

                用户描述：
                %s
                """.formatted(
                query == null ? "" : query);
        return chat(system, user);
    }

    private String difficultKnowledgePrompt(String query) {
        return """
                原始query如下：
                ---
                %s
                ---

                请结合知识库中相关内容，对该query进行回复。如果与知识点相关，列出在在知识库中找到的相关信息，并说明与该主题密切相关的重难点、易错点以及建议补充学习的知识点；若知识库无直接依据，先声明此次回答是“模型生成结果”，随后直接给出回答。如果是与知识类内容无关的闲聊，请直接拒绝回答。
                """.formatted(query == null ? "" : query);
    }

    /**
     * 结合向量库检索回答重难点与推荐知识（沿用 RagAssistant 的 RAG 管道）。
     */
    public String difficultKnowledge(String query, String courseId) {
        String prompt = difficultKnowledgePrompt(query);
        log.info("重难点推荐 query 长度: {}", prompt.length());
        return withRetrievalCourseId(courseId, () -> ragAssistant.chat(prompt));
    }

    public void difficultKnowledgeStream(String query, String courseId, Consumer<String> onToken, Consumer<Throwable> onError, Runnable onComplete) {
        String prompt = difficultKnowledgePrompt(query);
        log.info("重难点流式推荐 query 长度: {}", prompt.length());
        withRetrievalCourseId(courseId, () -> {
            streamingRagAssistant.chat(prompt)
                    .onNext(onToken)
                    .onError(onError)
                    .onComplete(response -> onComplete.run())
                    .start();
            return null;
        });
    }

    public String ask(String courseId, String question) {
        log.info("收到问题 courseId={} question={}", courseId, question);
        String answer = withRetrievalCourseId(courseId, () -> ragAssistant.chat(question));
        log.info("回答: {}", answer);
        return answer;
    }
}
