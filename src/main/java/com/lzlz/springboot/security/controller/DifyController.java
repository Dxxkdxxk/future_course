package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.service.DifyService;
import com.lzlz.springboot.security.service.DeepSeekService;
import com.lzlz.springboot.security.service.QuestionService;
import com.lzlz.springboot.security.dto.QuestionDto;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/course")
public class DifyController {

    @Autowired
    private DifyService difyService;

    @Autowired
    private DeepSeekService deepseekService;

    @Autowired
    private QuestionService questionService;

    //批改作业
    @PostMapping("/{courseId}/homeworkCorrect")
    public CompletableFuture<ResponseEntity<?>> uploadHomeworkCorrect(
            @PathVariable String courseId,
            @RequestParam("file") MultipartFile file) {

        // 验证文件是否为空
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("文件不能为空"));
        }
        Map<String, Object> response = new HashMap<>();
        
        // todo: 这里可以增加其他业务逻辑，目前为了节约dify额度先改成用ds了
        return deepseekService.DSMarkHomeworkAsync(file, courseId)
                .thenApply(difyResponse -> {
                    response.put("code", 200);
                    response.put("msg", "批改成功");
                    response.put("data", difyResponse);
                    return ResponseEntity.status(200).body(response);
                })
                .exceptionally(ex -> {
                    response.put("code", 400);
                    response.put("msg", "批改失败: " + ex.getMessage());
                    return ResponseEntity.status(400).body(response);
                });
    }

    //批改实验
    @PostMapping("/{courseId}/expCorrect")
    public CompletableFuture<ResponseEntity<?>> uploadExpCorrect(
            @PathVariable String courseId,
            @RequestParam("file") MultipartFile file) {
        
        // 验证文件是否为空
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("文件不能为空"));
        }
        Map<String, Object> response = new HashMap<>();
        
        // todo: 这里可以增加其他业务逻辑，目前为了节约dify额度先改成用ds了
        return deepseekService.DSMarkHomeworkAsync(file, courseId)
                .thenApply(difyResponse -> {
                    response.put("code", 200);
                    response.put("msg", "批改成功");
                    response.put("data", difyResponse);
                    return ResponseEntity.status(200).body(response);
                })
                .exceptionally(ex -> {
                    response.put("code", 400);
                    response.put("msg", "批改失败: " + ex.getMessage());
                    return ResponseEntity.status(400).body(response);
                });
    }

    //生成题库
    @PostMapping("/{courseId}/question/aigenerate")
    public CompletableFuture<ResponseEntity<?>> questionAIgenerate(
            @PathVariable String courseId,
            @RequestBody Map<String, Object> jsonData
    ) {

        Map<String, Object> response = new HashMap<>();

        String query = (String) jsonData.get("questionQuery");
        return difyService.DifyQueGeneAsync(query, courseId)
                .thenApply(difyResponse -> {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode questionsArray = objectMapper.readTree(difyResponse);

                        if (!questionsArray.isArray()) {
                            response.put("code", 400);
                            response.put("msg", "AI返回的不是有效的题目数组");
                            return ResponseEntity.status(400).body(response);
                        }

                        int successCount = 0;
                        int failCount = 0;
                        int i = 0;

                        // 遍历每个题目，直接调用 QuestionService 创建（保持串行，避免打爆DB连接/事务）
                        for (JsonNode questionNode : questionsArray) {
                            try {
                                i++;
                                QuestionDto.CreateRequest req = new QuestionDto.CreateRequest();
                                req.setStem(questionNode.path("stem").asText(null));
                                req.setType(questionNode.path("type").asText(null));
                                req.setTopic(questionNode.path("topic").asText(null));
                                if (questionNode.has("difficulty")) {
                                    req.setDifficulty(questionNode.get("difficulty").asText());
                                }
                                if (questionNode.has("score")) {
                                    req.setScore(questionNode.get("score").isInt()
                                            ? questionNode.get("score").asInt()
                                            : null);
                                }
                                if (questionNode.has("estimatedTime")) {
                                    req.setEstimatedTime(questionNode.get("estimatedTime").isInt()
                                            ? questionNode.get("estimatedTime").asInt()
                                            : null);
                                }
                                if (questionNode.has("answer")) {
                                    req.setAnswer(questionNode.get("answer").asText());
                                }
                                if (questionNode.has("analysis")) {
                                    req.setAnalysis(questionNode.get("analysis").asText());
                                }

                                questionService.createQuestion(Long.valueOf(courseId), req);
                                successCount++;
                                System.out.println("成功创建题目 " + i + "：" + questionNode);
                            } catch (Exception e) {
                                failCount++;
                                System.out.println("题目 " + i + " 创建失败：" + e.getMessage());
                            }
                        }

                        response.put("code", 200);
                        response.put("msg", "成功创建" + successCount + "题，失败" + failCount + "题");
                        Map<String, Object> data = new HashMap<>();
                        data.put("response", difyResponse);
                        response.put("data", data);

                        return ResponseEntity.status(200).body(response);
                    } catch (Exception e) {
                        response.put("code", 400);
                        response.put("msg", "题目生成失败: " + e.getMessage());
                        return ResponseEntity.status(400).body(response);
                    }
                })
                .exceptionally(ex -> {
                    response.put("code", 400);
                    response.put("msg", "题目生成失败: " + ex.getMessage());
                    return ResponseEntity.status(400).body(response);
                });
    }

    //生成教材大纲
    @PostMapping("/{courseId}/textbooks/{textbookId}/chapters/outline")//todo：改了
    public CompletableFuture<ResponseEntity<?>> outlineAIgenerate(
            @PathVariable String courseId,
            @PathVariable String textbookId,
            @RequestParam("file") MultipartFile file) {

        // 验证文件是否为空
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("文件不能为空"));
        }
        Map<String, Object> response = new HashMap<>();
        
        return deepseekService.outlineDSAsync(file)
                .thenApply(deepseekResponse -> {
                    response.put("code", 200);
                    response.put("msg", "生成大纲成功");
                    response.put("data", deepseekResponse);
                    return ResponseEntity.status(200).body(response);
                })
                .exceptionally(ex -> {
                    response.put("code", 400);
                    response.put("msg", "生成大纲失败: " + ex.getMessage());
                    return ResponseEntity.status(400).body(response);
                });
    }

    //生成教材富媒体
    @PostMapping("/{courseId}/textbooks/{textbookId}/richmedia")//todo：改了
    public CompletableFuture<ResponseEntity<?>> getRichMedia(
            @PathVariable String courseId,
            @PathVariable String textbookId,
            @RequestBody Map<String, Object> jsonData) {

        Map<String, Object> response = new HashMap<>();
        
        String query = (String) jsonData.get("content");
        return deepseekService.richMediaDSAsync(query)
                .thenApply(deepseekResponse -> {
                    response.put("code", 200);
                    response.put("msg", "生成教材富媒体成功");
                    response.put("data", deepseekResponse);
                    return ResponseEntity.status(200).body(response);
                })
                .exceptionally(ex -> {
                    response.put("code", 400);
                    response.put("msg", "生成教材富媒体失败: " + ex.getMessage());
                    return ResponseEntity.status(400).body(response);
                });
    }
    
    //获取教材重难点推荐知识
    @PostMapping("/{courseId}/textbooks/{textbookId}/difficult")
    public CompletableFuture<ResponseEntity<?>> getDifficult(
            @PathVariable String courseId,
            @PathVariable String textbookId,
            @RequestBody Map<String, Object> jsonData
            ) {

        Map<String, Object> response = new HashMap<>();

        String query = (String) jsonData.get("content");
        return difyService.DifyDifficultAsync(query, courseId)
                .thenApply(difyResponse -> {
                    response.put("code", 200);
                    response.put("msg", "返回重难点推荐知识成功");
                    response.put("data", difyResponse);
                    return ResponseEntity.status(200).body(response);
                })
                .exceptionally(ex -> {
                    response.put("code", 400);
                    response.put("msg", "重难点推荐知识生成失败: " + ex.getMessage());
                    return ResponseEntity.status(400).body(response);
                });
    }
}