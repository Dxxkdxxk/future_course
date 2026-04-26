package com.lzlz.springboot.security.rag;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * 单次 HTTP 请求内的 RAG 检索范围：供 {@link dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever}
 * 的 dynamicFilter 读取，限定 pgvector 元数据 {@code courseId}。
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RagRetrievalContext {

    /**
     * 与任意真实 courseId 都不应冲突；未设置课程时用于过滤，使检索结果为空。
     */
    public static final String UNSCOPED_SENTINEL = "__RAG_NO_COURSE_SCOPE__";

    private String courseId;

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    /**
     * @return trim 后的课程 ID；若未设置或非空白则返回 {@link #UNSCOPED_SENTINEL}
     */
    public String effectiveCourseIdForFilter() {
        if (courseId == null) {
            return UNSCOPED_SENTINEL;
        }
        String t = courseId.trim();
        return t.isEmpty() ? UNSCOPED_SENTINEL : t;
    }

    public void clear() {
        this.courseId = null;
    }
}
