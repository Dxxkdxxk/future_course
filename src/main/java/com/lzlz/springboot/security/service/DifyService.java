package com.lzlz.springboot.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class DifyService {

    //todo:这里在src\main\resources\application.properties定义，url只有一个，key是每个智能体一个，每个数据库一个
    @Value("${dify.api.url}")
    private String difyApiUrl;

    @Value("${dify.api.homeworkkey}")
    private String difyApiHomeworkKey;

    @Value("${dify.api.expkey}")
    private String difyApiExpKey;

    @Value("${dify.api.Databasekey}")
    private String difyDatabaseKey;
    
    @Value("${dify.api.questiongenekey}")
    private String difyQuestionGeneKey;

    @Value("${dify.api.chatkey}")
    private String difyChatKey;


    //todo:这里只有一个课程的数据库，所以可以先写死
    private String databaseId = "e312e67c-7a05-4afa-9059-6a0d85cba359";

    @Autowired
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // todo:目前先固定用户ID，后面可以看情况改
    private static final String USER_ID = "user-homework-system";

    /**
     * 智能体批改作业
     */
    public String DifyMarkHomework(MultipartFile file, String courseId) {
        try {
            System.out.println("\n========== 开始处理文件 ==========");

            // 上传文件
            DifyFileInfo info = uploadFileToDify(file, difyApiHomeworkKey);
            if (info == null) {
                return "文件上传到 Dify 失败";
            }

            System.out.println("✅ 文件上传成功，file_id = " + info.fileId);
            System.out.println("✅ 文件 URL = " + info.sourceUrl);

            String answer = sendFileChatRequest(
                    difyApiHomeworkKey,
                    courseId,
                    info.sourceUrl,
                    "请分析这个文档的内容，并提供摘要"
                    //todo: query，从后端读，理论上可以由教师指定处理作业文件的query，这里先写死
            );

            System.out.println("========== 处理完成 ==========\n");
            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败：" + e.getMessage();
        }
    }

    /**
     * 智能体批改实验
     */
    public String DifyMarkExp(MultipartFile file, String courseId) {
        try {
            System.out.println("\n========== 开始处理文件 ==========");

            // 上传文件
            DifyFileInfo info = uploadFileToDify(file, difyApiExpKey);
            if (info == null) {
                return "文件上传到 Dify 失败";
            }

            System.out.println("✅ 文件上传成功，file_id = " + info.fileId);
            System.out.println("✅ 文件 URL = " + info.sourceUrl);

            String answer = sendFileChatRequest(
                    difyApiExpKey,
                    courseId,
                    info.sourceUrl,
                    "请分析这个文档的内容，并提供摘要"
                    //todo: query，从后端读，理论上可以由教师指定处理作业文件的query，这里先写死
            );

            System.out.println("========== 处理完成 ==========\n");
            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败：" + e.getMessage();
        }
    }

    /**
     * 智能体题库生成
     */
    public String DifyQueGene(String query, String courseId) {
        try {
            System.out.println("\n========== 开始生成题目 ==========");

            String answer = sendChatRequest(
                    difyQuestionGeneKey,
                    courseId,
                    query
            );

            System.out.println("========== 处理完成 ==========\n");
            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败：" + e.getMessage();
        }
    }

    /**
     * 智能体重难点联动
     */
    public String DifyDifficult(String query, String courseId) {
        try {
            System.out.println("\n========== 开始生成重难点推荐知识 ==========");

            String answer = sendChatRequest(
                    difyChatKey,
                    courseId,
                    query
            );

            System.out.println("========== 处理完成 ==========\n");
            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败：" + e.getMessage();
        }
    }


    /**
     * 上传文件（普通上传接口 /files/upload）
     * 返回 file_id + source_url
     */
    private DifyFileInfo uploadFileToDify(MultipartFile file, String difyApiKey) {
        try {
            System.out.println("\n--- 步骤1：上传文件 ---");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + difyApiKey);

            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

            // 文件内容
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            form.add("file", fileResource);
            form.add("user", USER_ID);

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(form, headers);

            String uploadUrl = difyApiUrl + "/files/upload";

            ResponseEntity<String> response =
                    restTemplate.postForEntity(uploadUrl, request, String.class);

            System.out.println("响应状态：" + response.getStatusCode());
            System.out.println("响应内容：" + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("❌ 上传失败");
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.has("data") ? root.get("data") : root;

            if (!data.has("id") || !data.has("source_url")) {
                System.err.println("❌ 返回内容缺少 file_id 或 source_url");
                return null;
            }

            DifyFileInfo info = new DifyFileInfo();
            info.fileId = data.get("id").asText();
            info.sourceUrl = data.get("source_url").asText();
            return info;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发送不带文件的请求到智能体
     */
    private String sendChatRequest(String difyApiKey, String courseId, String query) {
        try {
            System.out.println("\n--- 发送 chat-messages 请求 ---");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + difyApiKey);

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> input = new HashMap<>();
            input.put("course_id", courseId);
            body.put("inputs", input);
            body.put("query", query);
            body.put("response_mode", "blocking");
            body.put("conversation_id", "");
            body.put("user", USER_ID);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    headers
            );

            String url = difyApiUrl + "/chat-messages";

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            System.out.println("响应状态：" + response.getStatusCode());
            System.out.println("响应内容：" + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Dify 返回错误：" + response.getBody();
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("message") && root.get("message").has("content")) {
                return root.get("message").get("content").asText();
            }

            // ✅ 新版结构
            if (root.has("data")) {
                JsonNode data = root.get("data");

                if (data.has("outputs") && data.get("outputs").has("text")) {
                    return data.get("outputs").get("text").asText();
                }
            }

            // ✅ 旧版
            if (root.has("answer")) {
                return root.get("answer").asText();
            }

            return "未找到 AI 返回内容";

        } catch (Exception e) {
            e.printStackTrace();
            return "发送消息失败：" + e.getMessage();
        }
    }

    /**
     * 发送带文件的请求到智能体
     */
    private String sendFileChatRequest(String difyApiKey, String courseId, String sourceUrl, String query) {
        try {
            System.out.println("\n--- 步骤2：发送 chat-messages 请求 ---");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + difyApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            body.put("response_mode", "blocking");
            body.put("conversation_id", "");
            body.put("user", USER_ID);

            Map<String, Object> input = new HashMap<>();
            input.put("course_id", courseId);
            
            Map<String, Object> fileObj = new HashMap<>();
            fileObj.put("type", "document");
            fileObj.put("transfer_method", "remote_url");
            fileObj.put("url", sourceUrl);

            input.put("reportFile", fileObj);

            body.put("inputs", input);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    headers
            );

            String url = difyApiUrl + "/chat-messages";

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            System.out.println("响应状态：" + response.getStatusCode());
            System.out.println("响应内容：" + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Dify 返回错误：" + response.getBody();
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("message") && root.get("message").has("content")) {
                return root.get("message").get("content").asText();
            }

            // ✅ 新版结构
            if (root.has("data")) {
                JsonNode data = root.get("data");

                if (data.has("outputs") && data.get("outputs").has("text")) {
                    return data.get("outputs").get("text").asText();
                }
            }

            // ✅ 旧版
            if (root.has("answer")) {
                return root.get("answer").asText();
            }

            return "未找到 AI 返回内容";

        } catch (Exception e) {
            e.printStackTrace();
            return "发送消息失败：" + e.getMessage();
        }
    }

    /**
     * 上传文件到知识库
     */
    public String sendFileToDatabase(MultipartFile file, String courseId) {
        try {
            //todo:这整个函数应该根据courseId来判断difyDatabaseKey和databaseId是什么，但是目前只有一个课程，可以先写死

            System.out.println("\n--- 上传文件到知识库 ---");

            // 1. 构建data参数的JSON
            //todo:实测传pdf会出现不解析的问题，所以这个部分可能会需要配置处理文件的一些参数，目前最好先用word去测
            Map<String, Object> dataJson = new HashMap<>();
            dataJson.put("indexing_technique", "high_quality");
            dataJson.put("process_rule", Map.of("mode", "automatic"));
            
            String dataJsonString = objectMapper.writeValueAsString(dataJson);

            // 2. 将MultipartFile转换为Resource
            Resource fileResource = new MultipartFileResource(file);

            // 3. 创建multipart请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("data", dataJsonString);
            body.add("file", fileResource); // 使用Resource而不是MultipartFile

            // 4. 设置headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + difyDatabaseKey);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            // 5. API URL
            String url = difyApiUrl + "/datasets/"+ databaseId +"/document/create-by-file";
            
            System.out.println("请求URL: " + url);
            System.out.println("data参数: " + dataJsonString);
            System.out.println("文件名: " + file.getOriginalFilename());
            System.out.println("文件大小: " + file.getSize() + " bytes");

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            System.out.println("响应状态：" + response.getStatusCode());
            System.out.println("响应内容：" + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Dify 知识库上传返回错误：" + response.getBody();
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            // 处理成功响应
            if (root.has("document")) {
                JsonNode document = root.get("document");
                String documentId = document.has("id") ? document.get("id").asText() : "未知";
                String documentName = document.has("name") ? document.get("name").asText() : "未知";
                
                return String.format("文件上传成功！文档ID: %s, 文档名称: %s", documentId, documentName);
            }

            return "文件上传成功！响应: " + root.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "上传文件到知识库失败：" + e.getMessage();
        }

    }
    
    /**
     * 获取知识库文件列表
     */
    public Map<String, Object> getDocumentList(String courseId) throws Exception {
        //todo:这整个函数应该根据courseId来判断difyDatabaseKey和databaseId是什么，但是目前只有一个课程，可以先写死

        String url = difyApiUrl + "/datasets/" + databaseId + "/documents";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + difyDatabaseKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        return response.getBody();
    }

    // 添加这个内部类或独立的工具类
    private static class MultipartFileResource extends ByteArrayResource {
        private final String filename;

        public MultipartFileResource(MultipartFile multipartFile) throws IOException {
            super(multipartFile.getBytes());
            this.filename = multipartFile.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }

    /**
     * 存储上传后返回的 file_id & source_url
     */
    private static class DifyFileInfo {
        public String fileId;
        public String sourceUrl;
    }
}
