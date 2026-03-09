package com.lzlz.springboot.security.domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import cn.hutool.json.JSONUtil;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// WebSocket消息处理器（维护连接+推送消息）
@Component
@Slf4j
public class ParseCallbackHandler extends TextWebSocketHandler {
    // 存储用户连接：key=上传者ID（教师ID），value=WebSocket会话
    private final Map<Long, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    // 连接建立时触发
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从连接参数中获取上传者ID（前端连接时携带：ws://xxx?uploaderId=1001）
        String uploaderIdStr = session.getUri().getQuery().split("=")[1];
        Long uploaderId = Long.parseLong(uploaderIdStr);
        sessionMap.put(uploaderId, session);
        log.info("WebSocket连接建立：uploaderId={}", uploaderId);
    }

    // 推送分节结果给前端
    public void pushParseResult(Long uploaderId, TextbookParseResult result) {
        WebSocketSession session = sessionMap.get(uploaderId);
        if (session != null && session.isOpen()) {
            try {
                // 发送JSON格式消息
                String json = JSONUtil.toJsonStr(result);
                session.sendMessage(new TextMessage(json));
                log.info("推送分节结果给uploaderId={}：{}", uploaderId, json);
            } catch (IOException e) {
                log.error("推送分节结果失败", e);
            }
        }
    }

    // 连接关闭时移除会话
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionMap.values().remove(session);
        log.info("WebSocket连接关闭");
    }

    // 分节结果DTO（用于前端展示）
    @Data
    public static class TextbookParseResult {
        private Long textbookId;    // 教材ID
        private String textbookName;// 教材名称
        private String status;      // 分节状态：SUCCESS/FAIL
        private String msg;         // 提示信息
        private Integer chapterCount; // 解析出的章节数
    }
}
