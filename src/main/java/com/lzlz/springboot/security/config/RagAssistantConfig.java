package com.lzlz.springboot.security.config;

import com.lzlz.springboot.security.assistant.RagAssistant;
import com.lzlz.springboot.security.assistant.StreamingRagAssistant;
import com.lzlz.springboot.security.rag.RagRetrievalContext;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Configuration
public class RagAssistantConfig {

    @Bean
    public RagAssistant ragAssistant(
            ChatLanguageModel chatLanguageModel,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            RagRetrievalContext ragRetrievalContext) {

        ContentRetriever retriever = contentRetriever(embeddingModel, embeddingStore, ragRetrievalContext);

        return AiServices.builder(RagAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .contentRetriever(retriever)
                .build();
    }

    @Bean
    public StreamingRagAssistant streamingRagAssistant(
            StreamingChatLanguageModel streamingChatLanguageModel,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            RagRetrievalContext ragRetrievalContext) {

        ContentRetriever retriever = contentRetriever(embeddingModel, embeddingStore, ragRetrievalContext);

        return AiServices.builder(StreamingRagAssistant.class)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .contentRetriever(retriever)
                .build();
    }

    private ContentRetriever contentRetriever(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            RagRetrievalContext ragRetrievalContext) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.6)
                .dynamicFilter(query -> metadataKey("courseId").isEqualTo(ragRetrievalContext.effectiveCourseIdForFilter()))
                .build();
    }
}
