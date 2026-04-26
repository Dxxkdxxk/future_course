package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.assistant.RagAssistant;
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

    /**
     * 结合向量库检索回答重难点与推荐知识（沿用 RagAssistant 的 RAG 管道）。
     */
    public String difficultKnowledge(String query, String courseId) {
        String prompt = """
                重难点主题如下：
                ---
                %s
                ---

                请结合知识库中相关内容，列出与该主题密切相关的重难点、易错点以及建议补充学习的知识点；若知识库无直接依据，请明确说明并给出通用学习建议。
                """.formatted(query == null ? "" : query);
        log.info("重难点推荐 query 长度: {}", prompt.length());
        return withRetrievalCourseId(courseId, () -> ragAssistant.chat(prompt));
    }

    public String ask(String courseId, String question) {
        log.info("收到问题 courseId={} question={}", courseId, question);
        String answer = withRetrievalCourseId(courseId, () -> ragAssistant.chat(question));
        log.info("回答: {}", answer);
        return answer;
    }
}
