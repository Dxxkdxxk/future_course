package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.dto.QuestionDto;
import com.lzlz.springboot.security.service.QuestionService;
import com.lzlz.springboot.security.service.RagDocumentService;
import com.lzlz.springboot.security.service.RagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/course"})
@RequiredArgsConstructor
public class RagController {

    private final RagDocumentService documentService;
    private final RagService ragService;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    /**
     * 上传文档入库（向量 metadata 带 courseId，检索仅在同课程文档内匹配）
     */
    @PostMapping("/document")
    public ResponseEntity<Map<String, String>> uploadDocument(
            @RequestParam("courseId") String courseId,
            @RequestParam("file") MultipartFile file) throws Exception {
        try {
            RagDocumentService.DocumentInfo docInfo = documentService.ingestDocument(file, courseId);
            Map<String, String> body = new LinkedHashMap<>();
            body.put("message", "文档上传成功");
            body.put("courseId", docInfo.getCourseId());
            body.put("filename", docInfo.getFilename());
            body.put("fileId", docInfo.getFileId());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "文档向量化失败，请确认文档包含可解析文本（非扫描图片）",
                    "detail", e.getMessage() == null ? "unknown" : e.getMessage()
            ));
        }
    }

    /**
     * 判断某个文件是否已加入知识库（按 fileId）；传 courseId 时要求记录归属一致
     */
    @GetMapping("/document/exists")
    public ResponseEntity<Map<String, Object>> existsDocument(
            @RequestParam("fileId") String fileId,
            @RequestParam(value = "courseId", required = false) String courseId) {
        boolean exists = documentService.existsByFileId(fileId, courseId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fileId", fileId);
        body.put("exists", exists);
        if (courseId != null && !courseId.isBlank()) {
            body.put("courseId", courseId.trim());
        }
        return ResponseEntity.ok(body);
    }

    /**
     * 从向量库删除某个文件（按 fileId）；传 courseId 时仅删除该课程下的向量与索引记录
     */
    @DeleteMapping("/document")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @RequestParam("fileId") String fileId,
            @RequestParam(value = "courseId", required = false) String courseId) {
        try {
            boolean removed = documentService.deleteByFileId(fileId, courseId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fileId", fileId);
            body.put("deleted", true);
            body.put("removedFromIndex", removed);
            if (courseId != null && !courseId.isBlank()) {
                body.put("courseId", courseId.trim());
            }
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                    "error", "当前向量库不支持按 metadata 删除（removeAll(filter)）",
                    "detail", e.getMessage() == null ? "unknown" : e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "删除向量失败",
                    "detail", e.getMessage() == null ? "unknown" : e.getMessage()
            ));
        }
    }

    /**
     * 知识库问答（仅检索该 courseId 下入库文档）
     * Body: { "courseId": "课程ID", "question": "你的问题" }
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(
            @RequestBody Map<String, String> body) {
        String courseId = body.get("courseId");
        String question = body.get("question");
        if (courseId == null || courseId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "courseId 不能为空"));
        }
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "问题不能为空"));
        }
        String answer = ragService.ask(courseId.trim(), question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    /**
     * 查看已上传文档清单；可选 courseId 仅列出该课程
     */
    @GetMapping("/checkDocument")
    public ResponseEntity<List<Map<String, String>>> checkDocument(
            @RequestParam(value = "courseId", required = false) String courseId) {
        List<Map<String, String>> result = documentService.listIngestedDocuments(courseId)
                .stream()
                .map(doc -> {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("fileId", doc.getFileId());
                    row.put("filename", doc.getFilename());
                    if (doc.getCourseId() != null) {
                        row.put("courseId", doc.getCourseId());
                    }
                    return row;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    // ========== 以下接口与原 future_course DifyController 路径兼容（LangChain4j 实现）==========

    @PostMapping("/{courseId}/homeworkCorrect")
    public ResponseEntity<?> uploadHomeworkCorrect(
            @PathVariable String courseId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }
        Map<String, Object> response = new HashMap<>();
        try {
            String text = ragService.markHomework(file, courseId);
            response.put("code", 200);
            response.put("msg", "批改成功");
            response.put("data", text);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("msg", "批改失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{courseId}/expCorrect")
    public ResponseEntity<?> uploadExpCorrect(
            @PathVariable String courseId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }
        Map<String, Object> response = new HashMap<>();
        try {
            String text = ragService.markExperiment(file, courseId);
            response.put("code", 200);
            response.put("msg", "批改成功");
            response.put("data", text);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("msg", "批改失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{courseId}/question/aigenerate")
    public ResponseEntity<?> questionAIgenerate(
            @PathVariable String courseId,
            @RequestBody Map<String, Object> jsonData) {
        Map<String, Object> response = new HashMap<>();
        try {
            String query = (String) jsonData.get("questionQuery");
            String aiResponse = ragService.generateQuestions(query, courseId);
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode questionsArray = root.isArray() ? root : null;
            if (questionsArray == null && root.isObject() && root.has("questions") && root.get("questions").isArray()) {
                questionsArray = root.get("questions");
            }
            if (questionsArray == null || !questionsArray.isArray()) {
                response.put("code", 400);
                response.put("msg", "AI返回的不是有效的题目数组");
                return ResponseEntity.badRequest().body(response);
            }
            int successCount = 0;
            int failCount = 0;
            for (JsonNode questionNode : questionsArray) {
                try {
                    QuestionDto.CreateRequest req = new QuestionDto.CreateRequest();
                    req.setStem(questionNode.path("stem").asText(null));
                    req.setType(questionNode.path("type").asText(null));
                    req.setTopic(questionNode.path("topic").asText(null));
                    if (questionNode.has("difficulty")) {
                        req.setDifficulty(questionNode.get("difficulty").asText());
                    }
                    if (questionNode.has("score") && questionNode.get("score").canConvertToInt()) {
                        req.setScore(questionNode.get("score").asInt());
                    }
                    if (questionNode.has("estimatedTime") && questionNode.get("estimatedTime").canConvertToInt()) {
                        req.setEstimatedTime(questionNode.get("estimatedTime").asInt());
                    }
                    if (questionNode.has("answer")) {
                        req.setAnswer(questionNode.get("answer").asText());
                    }
                    if (questionNode.has("analysis")) {
                        req.setAnalysis(questionNode.get("analysis").asText());
                    }
                    if (req.getStem() == null || req.getStem().isBlank()) {
                        failCount++;
                        continue;
                    }
                    questionService.createQuestion(Long.valueOf(courseId), req);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                }
            }
            response.put("code", 200);
            response.put("msg", "成功创建" + successCount + "题，失败" + failCount + "题");
            Map<String, Object> data = new HashMap<>();
            data.put("response", aiResponse);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("msg", "题目生成失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{courseId}/textbooks/{textbookId}/chapters/outline")
    public ResponseEntity<?> outlineAIgenerate(
            @PathVariable String courseId,
            @PathVariable String textbookId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }
        Map<String, Object> response = new HashMap<>();
        try {
            String text = ragService.outlineFromDocument(file, courseId, textbookId);
            response.put("code", 200);
            response.put("msg", "生成大纲成功");
            response.put("data", text);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("msg", "生成大纲失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{courseId}/textbooks/{textbookId}/richmedia")
    public ResponseEntity<?> getRichMedia(
            @PathVariable String courseId,
            @PathVariable String textbookId,
            @RequestBody Map<String, Object> jsonData) {
        Map<String, Object> response = new HashMap<>();
        try {
            String query = (String) jsonData.get("content");
            String text = ragService.richMediaRecommendations(query, courseId, textbookId);
            response.put("code", 200);
            response.put("msg", "生成教材富媒体成功");
            response.put("data", text);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("msg", "生成教材富媒体失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{courseId}/textbooks/{textbookId}/difficult")
    public ResponseEntity<?> getDifficult(
            @PathVariable String courseId,
            @PathVariable String textbookId,
            @RequestBody Map<String, Object> jsonData) {
        Map<String, Object> response = new HashMap<>();
        try {
            String query = (String) jsonData.get("content");
            String text = ragService.difficultKnowledge(query, courseId);
            response.put("code", 200);
            response.put("msg", "返回重难点推荐知识成功");
            response.put("data", text);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("msg", "重难点推荐知识生成失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping(value = "/{courseId}/textbooks/{textbookId}/difficult/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getDifficultStream(
            @PathVariable String courseId,
            @PathVariable String textbookId,
            @RequestBody Map<String, Object> jsonData) {
        SseEmitter emitter = new SseEmitter(120_000L);
        String query = (String) jsonData.get("content");
        ragService.difficultKnowledgeStream(
                query,
                courseId,
                token -> sendSse(emitter, "delta", Map.of("text", token)),
                error -> {
                    sendSse(emitter, "error", Map.of("code", 400, "msg", "重难点推荐知识生成失败: " + error.getMessage()));
                    emitter.completeWithError(error);
                },
                () -> {
                    sendSse(emitter, "done", Map.of("code", 200, "msg", "生成完成"));
                    emitter.complete();
                }
        );
        return emitter;
    }

    private void sendSse(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

}