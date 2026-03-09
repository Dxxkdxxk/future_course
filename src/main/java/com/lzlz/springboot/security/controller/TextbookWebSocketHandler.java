package com.lzlz.springboot.security.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import cn.hutool.core.net.Ipv4Util;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 教材解析WebSocket处理器：维护客户端连接，支持跨IP消息推送
 */
@Slf4j
@Component
public class TextbookWebSocketHandler extends TextWebSocketHandler {

    /**
     * 连接映射表：key=客户端标识（局域网IP+端口/用户ID），value=WebSocket会话
     * ConcurrentHashMap保证线程安全
     */
    private static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 客户端建立连接时触发（记录IP+会话）
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 获取客户端局域网IP（关键：从会话中解析远程IP）
        String clientIp = getClientIp(session);
        // 2. 生成唯一客户端标识（IP+会话ID，避免同一IP多客户端冲突）
        String clientKey = clientIp + "_" + session.getId();
        // 3. 保存连接映射
        SESSION_MAP.put(clientKey, session);
        log.info("客户端{}（IP：{}）建立WebSocket连接，当前在线数：{}",
                clientKey, clientIp, SESSION_MAP.size());

        // 可选：向客户端发送连接成功消息
        sendMessageToClient(session, "连接成功，你的局域网IP：" + clientIp);
    }

    /**
     * 接收客户端消息（支持转发给其他IP客户端）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到客户端{}消息：{}", getClientIp(session), payload);

        // 示例：解析消息，按目标IP推送（消息格式：{"targetIp":"192.168.1.100","content":"xxx"}）
        try {
            // 解析前端发送的JSON消息（可替换为FastJSON/Jackson）
            Map<String, String> msgMap = JSONUtil.toBean(payload, Map.class);
            String targetIp = msgMap.get("targetIp");
            String content = msgMap.get("content");

            // 向指定IP的所有客户端推送消息
            sendMessageToIp(targetIp, content);
        } catch (Exception e) {
            log.error("解析消息失败", e);
            sendMessageToClient(session, "消息格式错误：" + e.getMessage());
        }
    }

    /**
     * 连接关闭时触发（清理映射）
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String clientIp = getClientIp(session);
        String clientKey = clientIp + "_" + session.getId();
        SESSION_MAP.remove(clientKey);
        log.info("客户端{}（IP：{}）断开连接，当前在线数：{}",
                clientKey, clientIp, SESSION_MAP.size());
    }

    /**
     * 处理连接异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("客户端{}连接异常", getClientIp(session), exception);
        if (session.isOpen()) {
            session.close();
        }
        // 清理异常连接
        String clientKey = getClientIp(session) + "_" + session.getId();
        SESSION_MAP.remove(clientKey);
    }

    // ---------------------- 核心工具方法 ----------------------
    /**
     * 获取客户端局域网IP（关键：从WebSocket会话中解析远程地址）
     */
    private String getClientIp(WebSocketSession session) {
        // 从会话的远程地址中提取IP（格式：/192.168.1.100:54321）
        String remoteAddr = session.getRemoteAddress().toString();
        // 截取IP部分（去掉端口和斜杠）
        String ip = remoteAddr.substring(1, remoteAddr.indexOf(":"));
        // 过滤内网IP（可选：确保是局域网IP）
        if (Ipv4Util.isInnerIP(ip)) {
            return ip;
        }
        return ip;
    }

    /**
     * 向单个客户端发送消息
     */
    public void sendMessageToClient(WebSocketSession session, String content) {
        if (session == null || !session.isOpen()) {
            log.warn("客户端会话已关闭，无法发送消息");
            return;
        }
        try {
            session.sendMessage(new TextMessage(content));
        } catch (IOException e) {
            log.error("发送消息给客户端失败", e);
        }
    }

    /**
     * 向指定局域网IP的所有客户端推送消息（核心：跨IP推送）
     */
    public void sendMessageToIp(String targetIp, String content) {
        if (targetIp == null || content == null) {
            log.warn("目标IP或消息内容为空");
            return;
        }
        // 遍历连接映射，找到目标IP的所有客户端
        int sendCount = 0;
        for (Map.Entry<String, WebSocketSession> entry : SESSION_MAP.entrySet()) {
            String clientKey = entry.getKey();
            WebSocketSession session = entry.getValue();
            // 匹配目标IP（clientKey格式：192.168.1.100_xxxx）
            if (clientKey.startsWith(targetIp + "_") && session.isOpen()) {
                sendMessageToClient(session, content);
                sendCount++;
            }
        }
        log.info("向IP{}推送消息，成功发送给{}个客户端，内容：{}", targetIp, sendCount, content);
    }

    /**
     * 广播消息（向所有局域网客户端推送）
     */
    public void broadcastMessage(String content) {
        for (WebSocketSession session : SESSION_MAP.values()) {
            sendMessageToClient(session, content);
        }
        log.info("广播消息给所有客户端，内容：{}", content);
    }

    // ---------------------- 对外提供的推送接口（供业务层调用） ----------------------
    /**
     * 业务层调用：向指定IP推送教材解析结果
     */
    public void pushParseResult(String targetIp, String result) {
        sendMessageToIp(targetIp, result);
    }
}
