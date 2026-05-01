package com.lzlz.springboot.security.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface StreamingRagAssistant {

    @SystemMessage("""
            你是一个专业的知识库助手，请严格基于提供的上下文内容回答用户问题。
            如果上下文中没有相关信息，请直接回答\"我在知识库中未找到相关信息\"，不要编造答案。
            回答请简洁、准确，使用中文。
            """)
    TokenStream chat(@UserMessage String question);
}
