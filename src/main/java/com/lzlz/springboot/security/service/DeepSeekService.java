package com.lzlz.springboot.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.annotation.Async;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class DeepSeekService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private RestTemplate restTemplate;

    private final String apiKey = "sk-29e6e19b34644fd3bd03acdb85c85ec3";
    private final String url = "https://api.deepseek.com/v1/chat/completions";

    /**
     * 异步版本：对齐项目其它长耗时任务（复用 AsyncConfig.taskExecutor）
     */
    @Async
    public CompletableFuture<String> outlineDSAsync(MultipartFile file) {
        return CompletableFuture.completedFuture(outlineDS(file));
    }

    /**
     * 异步版本：对齐项目其它长耗时任务（复用 AsyncConfig.taskExecutor）
     */
    @Async
    public CompletableFuture<String> richMediaDSAsync(String query) {
        return CompletableFuture.completedFuture(richMediaDS(query));
    }

    /**
     * 异步版本：对齐项目其它长耗时任务（复用 AsyncConfig.taskExecutor）
     */
    @Async
    public CompletableFuture<String> DSMarkHomeworkAsync(MultipartFile file, String courseId) {
        return CompletableFuture.completedFuture(DSMarkHomework(file, courseId));
    }


    // 教材大纲
    public String outlineDS(MultipartFile file) {
        try {
            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 4. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 5. 构建请求体 (使用Map自动处理转义)
            String prompt = "请根据上传的教材内容生成本章节大纲、章节概览、章节总结。文件内容如下：\n" + fileContent;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 2000);
            
            // 6. 发送请求
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            // 7. 解析响应
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0)
                                    .path("message").path("content").asText();
                return content;
            } else {
                return "API请求失败: " + response.getStatusCode();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败: " + e.getMessage();
        }
    }

    // 教材富媒体
    public String richMediaDS(String query) {
        try {           
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 构建请求体 (使用Map自动处理转义)
            String prompt1 = "你是一个富媒体内容推荐智能体。请理解用户query，识别其中的关键实体、场景、情绪或动作关键词，然后推荐最合适的富媒体资源，包括图片、视频、音频、GIF等。\n\n" +
               "输出格式要求：\n" +
               "请严格回复纯JSON格式文本，不要带有“```json```”，包含以下字段：\n" +
               "{\n" +
               "  \"query_analysis\": \"简短分析query的意图和关键词\",\n" + 
               "  \"recommendations\": [\n" +
               "    {\n" +
               "      \"media_type\": \"图片/视频/音频/GIF\",\n" +
               "      \"description\": \"推荐理由和内容描述\",\n" +
               "      \"source_url\": \"示例链接（严格要求确实存在，禁止编造）\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"tips\": \"给用户的额外建议\"\n" +
               "}" +
               " 用户query是：" + query;

            String prompt2 = "你是一个富媒体内容推荐智能体。请理解用户query，识别其中的关键实体、场景、情绪或动作关键词，然后推荐最合适的富媒体资源，包括图片、视频、音频、GIF等，以网页形式提供，链接严格要求确实存在，禁止编造。\n\n" +
               "输出时每条严格要求按照以下格式：\n" +
               "[序号]\n" +
               "推荐链接：\n" + 
               "推荐理由：\n" +
               "用户query是：" + query;

            String prompt = prompt2;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 2000);
            
            // 发送请求
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0)
                                    .path("message").path("content").asText();

                return content;

            } else {
                return "API请求失败: " + response.getStatusCode();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败: " + e.getMessage();
        }
    }

     /**
     * 智能体批改作业
     */
    public String DSMarkHomework(MultipartFile file, String courseId) {
        try {
            System.out.println("\n========== 开始DS批改作业 ==========");

            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            String prompt = "你是一个实验报告批改助手。这是学生的实验报告：\n"
        + fileContent
        + "\n你需要给用户上传的实验报告打分，满分100。必须给出分数和评分理由，格式为：【评分：xx分。理由：xxx】。打分依据是实验报告的内容是否正确，结构是否完整，可以检索外部信息进行判断。";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 2000);
            
            // 6. 发送请求
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            // 7. 解析响应
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0)
                                    .path("message").path("content").asText();
                System.out.println("========== DS批改作业完成 ==========\n");
                return content;
            } else {
                return "API请求失败: " + response.getStatusCode();
            }
            

        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败：" + e.getMessage();
        }
    }
}