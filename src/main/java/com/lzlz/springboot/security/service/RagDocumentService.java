package com.lzlz.springboot.security.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentService {

    private final EmbeddingStoreIngestor ingestor;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ObjectMapper objectMapper;
    @Value("${rag.document-index.path:./data/rag-documents.json}")
    private String documentIndexPath;
    private final List<DocumentInfo> ingestedDocuments = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void loadDocumentIndex() {
        Path path = Path.of(documentIndexPath);
        if (!Files.exists(path)) {
            return;
        }
        try {
            List<DocumentInfo> saved = objectMapper.readValue(path.toFile(), new TypeReference<List<DocumentInfo>>() {
            });
            ingestedDocuments.clear();
            ingestedDocuments.addAll(saved);
            log.info("已加载历史文档记录 {} 条: {}", saved.size(), path.toAbsolutePath());
        } catch (Exception e) {
            log.warn("读取历史文档记录失败，将继续使用空记录列表: {}", path.toAbsolutePath(), e);
        }
    }

    /**
     * 从上传文件中提取纯文本（不入库），供批改/大纲等场景复用与 {@link #ingestDocument} 相同的解析规则。
     */
    public String extractPlainText(MultipartFile file) throws IOException {
        String filename = normalizeFilename(file.getOriginalFilename());
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        Path tempFile = Files.createTempFile("rag_extract_", "_" + filename);
        file.transferTo(tempFile);
        try {
            Document document = loadDocument(tempFile, filename);
            document = sanitizeDocument(document, filename, "extract-only", null);
            return document.text();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 上传文档并入库；向量片段 metadata 含 fileId、filename、courseId。
     */
    public DocumentInfo ingestDocument(MultipartFile file, String courseId) throws IOException {
        String filename = normalizeFilename(file.getOriginalFilename());
        if (filename == null || filename.isBlank()) throw new IllegalArgumentException("文件名不能为空");
        if (file.isEmpty()) throw new IllegalArgumentException("上传文件为空");
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("courseId 不能为空");
        }
        String normalizedCourseId = courseId.trim();
        String fileId = UUID.randomUUID().toString();

        Path tempFile = Files.createTempFile("rag_", "_" + filename);
        file.transferTo(tempFile);

        try {
            Document document = loadDocument(tempFile, filename);
            document = sanitizeDocument(document, filename, fileId, normalizedCourseId);
            ingestor.ingest(document);
            DocumentInfo info = new DocumentInfo(fileId, filename, normalizedCourseId);
            ingestedDocuments.add(info);
            persistDocumentIndex();
            log.info("文档入库成功: {} courseId={}", filename, normalizedCourseId);
            return info;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public List<DocumentInfo> listIngestedDocuments(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return List.copyOf(ingestedDocuments);
        }
        String c = courseId.trim();
        return ingestedDocuments.stream()
                .filter(d -> c.equals(d.getCourseId()))
                .toList();
    }

    public boolean existsByFileId(String fileId, String courseId) {
        if (fileId == null || fileId.isBlank()) {
            return false;
        }
        return ingestedDocuments.stream().anyMatch(d -> {
            if (!fileId.equals(d.getFileId())) {
                return false;
            }
            if (courseId == null || courseId.isBlank()) {
                return true;
            }
            return courseId.trim().equals(d.getCourseId());
        });
    }

    public boolean deleteByFileId(String fileId, String courseId) {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("fileId 不能为空");
        }
        Filter filter = metadataKey("fileId").isEqualTo(fileId);
        if (courseId != null && !courseId.isBlank()) {
            filter = filter.and(metadataKey("courseId").isEqualTo(courseId.trim()));
        }
        embeddingStore.removeAll(filter);
        boolean removed = ingestedDocuments.removeIf(d -> {
            if (!fileId.equals(d.getFileId())) {
                return false;
            }
            if (courseId == null || courseId.isBlank()) {
                return true;
            }
            return courseId.trim().equals(d.getCourseId());
        });
        if (removed) {
            persistDocumentIndex();
        }
        return removed;
    }

    private synchronized void persistDocumentIndex() {
        Path path = Path.of(documentIndexPath);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), new ArrayList<>(ingestedDocuments));
        } catch (Exception e) {
            log.warn("保存文档记录失败: {}", path.toAbsolutePath(), e);
        }
    }

    /**
     * 根据文件后缀选择对应解析器
     */
    private Document loadDocument(Path path, String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return FileSystemDocumentLoader.loadDocument(path, new ApachePdfBoxDocumentParser());
        }
        if (lower.endsWith(".docx") || lower.endsWith(".doc")
                || lower.endsWith(".pptx") || lower.endsWith(".ppt")
                || lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return FileSystemDocumentLoader.loadDocument(path, new ApachePoiDocumentParser());
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")) {
            return loadPlainTextWithCharsetDetection(path);
        }
        throw new IllegalArgumentException("暂不支持的文件类型: " + filename);
    }

    private Document loadPlainTextWithCharsetDetection(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            String text = decodeWithFallback(bytes, StandardCharsets.UTF_8, Charset.forName("GB18030"));
            return Document.from(text);
        } catch (IOException e) {
            throw new RuntimeException("读取文本文件失败", e);
        }
    }

    private Document sanitizeDocument(Document document, String filename, String fileId, String courseId) {
        String text = document.text();
        if (text == null) {
            throw new IllegalArgumentException("文档无法解析为文本: " + filename);
        }
        String cleaned = text.replace("\u0000", "").trim();
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("文档内容为空或仅包含不可解析内容: " + filename);
        }
        Metadata metadata = document.metadata() == null ? new Metadata() : document.metadata();
        metadata.put("fileId", fileId);
        metadata.put("filename", filename);
        if (courseId != null && !courseId.isBlank()) {
            metadata.put("courseId", courseId.trim());
        }
        return Document.from(cleaned, metadata);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInfo {
        private String fileId;
        private String filename;
        /** 历史 JSON 可能缺省，为 null 表示入库时尚未记录课程 */
        private String courseId;
    }

    private String decodeWithFallback(byte[] bytes, Charset primary, Charset fallback) {
        try {
            return primary.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ignored) {
            try {
                return fallback.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString();
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException("文本文件编码无法识别，建议保存为 UTF-8");
            }
        }
    }

    private String normalizeFilename(String rawFilename) {
        if (rawFilename == null) {
            return null;
        }
        String trimmed = rawFilename.trim();
        if (trimmed.isBlank()) {
            return trimmed;
        }
        // Some clients send UTF-8 bytes but server decodes them as GBK, resulting in mojibake.
        String fixedFromGbk = new String(trimmed.getBytes(Charset.forName("GBK")), StandardCharsets.UTF_8);
        if (looksLikeMojibake(trimmed) && !looksLikeMojibake(fixedFromGbk)) {
            return fixedFromGbk;
        }
        return trimmed;
    }

    private boolean looksLikeMojibake(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String markers = "ÃÂ�鍙鍔鍏鏂鏃鐨濂浠涓";
        int hit = 0;
        for (int i = 0; i < text.length(); i++) {
            if (markers.indexOf(text.charAt(i)) >= 0) {
                hit++;
            }
        }
        return hit >= 2;
    }
}