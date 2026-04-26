package com.lzlz.springboot.security.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class LangChain4jConfig {

    // ==================== Chat Model（DeepSeek） ====================

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${rag.deepseek.api-key}") String apiKey,
            @Value("${rag.deepseek.base-url}") String baseUrl,
            @Value("${rag.deepseek.chat-model}") String model) {

        // DeepSeek 兼容 OpenAI 协议，直接用 OpenAiChatModel 并替换 baseUrl
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.3)           // 知识库问答建议低温，减少幻觉
                .maxTokens(8192)            // 大纲/题库等长输出
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    // ==================== Embedding Model ====================

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${rag.embedding.provider:local-model}") String provider,
            @Value("${rag.embedding.local.model-path:C:/Users/Administrator/models/bge-small-zh-v1.5}") String localModelPath,
            @Value("${rag.embedding.local.tokenizer-path:C:/Users/Administrator/models/bge-small-zh-v1.5}") String localTokenizerPath,
            @Value("${rag.deepseek.api-key}") String apiKey,
            @Value("${rag.deepseek.base-url}") String baseUrl,
            @Value("${rag.deepseek.embedding-model}") String model) {
        String normalized = provider.trim().toLowerCase();
        if ("builtin-bge-small-zh".equals(normalized)) {
            return new BgeSmallZhEmbeddingModel();
        }
        if ("openai".equals(normalized)) {
            return OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey.trim())
                    .baseUrl(baseUrl.trim())
                    .modelName(model.trim())
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }
        // default: load ONNX model/tokenizer from local filesystem
        Path modelPath = resolveModelPath(localModelPath.trim());
        Path tokenizerPath = resolveTokenizerPath(localTokenizerPath.trim());
        if (modelPath != null && tokenizerPath != null) {
            return new OnnxEmbeddingModel(
                    modelPath.toString(),
                    tokenizerPath.toString(),
                    PoolingMode.MEAN
            );
        }
        // If local path is not ONNX-ready, gracefully fallback to built-in Chinese bge model.
        return new BgeSmallZhEmbeddingModel();
    }

    // ==================== pgvector 向量存储 ====================

    @Bean
    @ConditionalOnProperty(name = "rag.pgvector.enabled", havingValue = "true")
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${rag.embedding.dimension:512}") int embeddingDimension) {

        URI uri;
        try {
            uri = new URI(jdbcUrl.trim().replace("jdbc:", ""));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid PostgreSQL jdbc url: " + jdbcUrl, e);
        }
        String host = uri.getHost();
        int port = uri.getPort();
        String database = uri.getPath().replaceFirst("^/", "");

        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(username.trim())
                .password(password.trim())
                .table("rag_embeddings")        // 自动创建此表
                .dimension(embeddingDimension)
                .createTable(true)              // 首次自动建表
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "rag.pgvector.enabled", havingValue = "false", matchIfMissing = true)
    public EmbeddingStore<TextSegment> inMemoryEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    private Path resolveModelPath(String configuredPath) {
        Path path = Path.of(configuredPath);
        if (Files.isRegularFile(path)) {
            return path;
        }
        Path[] candidates = new Path[]{
                path.resolve("model.onnx"),
                path.resolve("bge-small-zh.onnx"),
                path.resolve("pytorch_model.onnx")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Path resolveTokenizerPath(String configuredPath) {
        Path path = Path.of(configuredPath);
        if (Files.isRegularFile(path)) {
            return path;
        }
        Path[] candidates = new Path[]{
                path.resolve("tokenizer.json"),
                path.resolve("bge-small-zh-tokenizer.json")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}