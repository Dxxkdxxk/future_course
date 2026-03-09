package com.lzlz.springboot.security;


import com.alibaba.fastjson2.JSONObject;
import com.lzlz.springboot.security.response.AnnotationAddRequest;
import com.lzlz.springboot.security.response.AnnotationResponse;
import com.lzlz.springboot.security.response.ExtendReadingAddRequest;
import com.lzlz.springboot.security.response.ExtendReadingResponse;
import com.lzlz.springboot.security.service.AnnotationService;
import com.lzlz.springboot.security.service.ExtendReadingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/**
 * 批注&拓展阅读功能测试
 */
@SpringBootTest
@ActiveProfiles("test") // 激活test配置
public class AnnotationAndExtendReadingTest {

    @Autowired
    private AnnotationService annotationService;

    @Autowired
    private ExtendReadingService extendReadingService;

    // 测试数据常量
    private static final String STUDENT_ID = "test_2001";
    private static final Long TEXTBOOK_ID = 1001L;
    private static final Long CHAPTER_ID = 102L;

    /**
     * 测试1：基本批注（高亮）添加&查询
     */
//    @Test
//    public void testBasicAnnotation() {
//        // 1. 构建高亮批注请求（基本批注）
//        AnnotationAddRequest highlightRequest = new AnnotationAddRequest();
//        highlightRequest.setStudentId(STUDENT_ID);
//        highlightRequest.setTextbookId(TEXTBOOK_ID);
//        highlightRequest.setChapterId(CHAPTER_ID);
//        highlightRequest.setAnnotationType("HIGHLIGHT"); // 高亮（基本批注）
//
//        // 构建位置信息
//        JSONObject position = new JSONObject();
//        position.put("page", 15);
//        position.put("x1", 100);
//        position.put("y1", 200);
//        position.put("x2", 300);
//        position.put("y2", 250);
//        highlightRequest.setPositionInfo(position);
//
//        // 构建样式
//        JSONObject style = new JSONObject();
//        style.put("color", "#FF0000");
//        style.put("opacity", 0.6);
//        highlightRequest.setStyle(style);
//        highlightRequest.setContent(""); // 高亮无内容
//
//        // 2. 调用添加方法
//        Long annotationId = annotationService.addAnnotation(highlightRequest);
//        Assertions.assertNotNull(annotationId, "高亮批注添加失败，ID为空");
//
//        // 3. 查询批注
////        List<AnnotationResponse> annotationList = annotationService.getAnnotationList(
////                STUDENT_ID, TEXTBOOK_ID, CHAPTER_ID);
////        Assertions.assertTrue(annotationList.size() > 0, "批注查询失败，列表为空");
//
//        // 4. 校验查询结果
////        AnnotationResponse highlightResp = annotationList.stream()
////                .filter(item -> item.getId().equals(annotationId))
////                .findFirst()
////                .orElse(null);
////        Assertions.assertNotNull(highlightResp, "未查询到新增的高亮批注");
////        Assertions.assertEquals("HIGHLIGHT", highlightResp.getAnnotationType());
////        Assertions.assertEquals("", highlightResp.getContent());
////        Assertions.assertEquals("#FF0000", highlightResp.getStyle().getString("color"));
//
//        System.out.println("基本批注（高亮）测试通过，批注ID：" + annotationId);
//    }

    /**
     * 测试2：扩展批注（重点/难点）添加&查询
     */
//    @Test
//    public void testExtendAnnotation() {
//        // 1. 构建重点批注请求（扩展批注）
//        AnnotationAddRequest keyPointRequest = new AnnotationAddRequest();
//        keyPointRequest.setStudentId(STUDENT_ID);
//        keyPointRequest.setTextbookId(TEXTBOOK_ID);
//        keyPointRequest.setChapterId(CHAPTER_ID);
//        keyPointRequest.setAnnotationType("KEY_POINT"); // 重点批注（扩展批注）
//
//        // 位置信息
//        JSONObject position = new JSONObject();
//        position.put("page", 16);
//        position.put("x1", 50);
//        position.put("y1", 100);
//        position.put("x2", 200);
//        position.put("y2", 150);
//        keyPointRequest.setPositionInfo(position);
//
//        // 样式
//        JSONObject style = new JSONObject();
//        style.put("color", "#FFFF00");
//        style.put("opacity", 0.8);
//        keyPointRequest.setStyle(style);
//        keyPointRequest.setContent("本章核心：SpringBoot自动配置原理"); // 扩展批注必填内容
//
//        // 2. 添加重点批注
//        Long keyPointId = annotationService.addAnnotation(keyPointRequest);
//        Assertions.assertNotNull(keyPointId, "重点批注添加失败");
//
//        // 3. 构建难点批注请求
//        AnnotationAddRequest difficultyRequest = new AnnotationAddRequest();
//        difficultyRequest.setStudentId(STUDENT_ID);
//        difficultyRequest.setTextbookId(TEXTBOOK_ID);
//        difficultyRequest.setChapterId(CHAPTER_ID);
//        difficultyRequest.setAnnotationType("DIFFICULTY"); // 难点批注（扩展批注）
//        difficultyRequest.setPositionInfo(position);
//        difficultyRequest.setStyle(style);
//        difficultyRequest.setContent("难点：Bean生命周期回调机制");
//
//        // 4. 添加难点批注
//        Long difficultyId = annotationService.addAnnotation(difficultyRequest);
//        Assertions.assertNotNull(difficultyId, "难点批注添加失败");
//
//        // 5. 查询扩展批注
//        List<AnnotationResponse> extendAnnotationList = annotationService.getAnnotationList(
//                STUDENT_ID, TEXTBOOK_ID, CHAPTER_ID);
//        // 校验重点批注
//        AnnotationResponse keyPointResp = extendAnnotationList.stream()
//                .filter(item -> item.getId().equals(keyPointId))
//                .findFirst()
//                .orElse(null);
//        Assertions.assertEquals("KEY_POINT", keyPointResp.getAnnotationType());
//        Assertions.assertEquals("本章核心：SpringBoot自动配置原理", keyPointResp.getContent());
//
//        // 校验难点批注
//        AnnotationResponse difficultyResp = extendAnnotationList.stream()
//                .filter(item -> item.getId().equals(difficultyId))
//                .findFirst()
//                .orElse(null);
//        Assertions.assertEquals("DIFFICULTY", difficultyResp.getAnnotationType());
//        Assertions.assertEquals("难点：Bean生命周期回调机制", difficultyResp.getContent());
//
//        System.out.println("扩展批注测试通过，重点批注ID：" + keyPointId + "，难点批注ID：" + difficultyId);
//    }

    /**
     * 测试3：拓展阅读（URL/TEXT/FILE）添加&查询&删除
     */
    @Test
    public void testExtendReading() {
        // ========== 测试1：URL类型拓展阅读 ==========
        ExtendReadingAddRequest urlRequest = new ExtendReadingAddRequest();
        urlRequest.setStudentId(STUDENT_ID);
        urlRequest.setTextbookId(TEXTBOOK_ID);
        urlRequest.setChapterId(CHAPTER_ID);
        urlRequest.setMaterialType("URL"); // 网页链接
        urlRequest.setTitle("SpringBoot官方文档");

        // 位置信息
        JSONObject urlPosition = new JSONObject();
        urlPosition.put("page", 15);
        urlPosition.put("x1", 100);
        urlPosition.put("y1", 200);
        urlRequest.setPositionInfo(urlPosition);

        urlRequest.setUrl("https://docs.spring.io/spring-boot/docs/current/reference/html/");

        // 添加URL类型拓展阅读
        Long urlReadingId = extendReadingService.addExtendReading(urlRequest);
        Assertions.assertNotNull(urlReadingId, "URL类型拓展阅读添加失败");

        // ========== 测试2：TEXT类型拓展阅读 ==========
        ExtendReadingAddRequest textRequest = new ExtendReadingAddRequest();
        textRequest.setStudentId(STUDENT_ID);
        textRequest.setTextbookId(TEXTBOOK_ID);
        textRequest.setChapterId(CHAPTER_ID);
        textRequest.setMaterialType("TEXT"); // 文本内容
        textRequest.setTitle("SpringBoot核心注解总结");
        textRequest.setPositionInfo(urlPosition);
        textRequest.setContent("@SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan");

        Long textReadingId = extendReadingService.addExtendReading(textRequest);
        Assertions.assertNotNull(textReadingId, "TEXT类型拓展阅读添加失败");

        // ========== 测试3：FILE类型拓展阅读（模拟MinIO文件） ==========
        ExtendReadingAddRequest fileRequest = new ExtendReadingAddRequest();
        fileRequest.setStudentId(STUDENT_ID);
        fileRequest.setTextbookId(TEXTBOOK_ID);
        fileRequest.setChapterId(CHAPTER_ID);
        fileRequest.setMaterialType("FILE"); // 文件类型
        fileRequest.setTitle("SpringBoot实战教程.pdf");
        fileRequest.setPositionInfo(urlPosition);
        // 模拟MinIO存储信息（实际需先上传文件到MinIO）
        fileRequest.setMinioBucket("textbook-bucket");
        fileRequest.setMinioObjectName("extend/2001/1001/springboot_tutorial.pdf");
        fileRequest.setFileSize(1024000L); // 1MB
        fileRequest.setFileType("PDF");

        Long fileReadingId = extendReadingService.addExtendReading(fileRequest);
        Assertions.assertNotNull(fileReadingId, "FILE类型拓展阅读添加失败");

        // ========== 查询拓展阅读 ==========
        List<ExtendReadingResponse> readingList = extendReadingService.getExtendReadingList(
                STUDENT_ID, TEXTBOOK_ID, CHAPTER_ID);
        Assertions.assertTrue(readingList.size() >= 3, "拓展阅读查询失败");

        // 校验URL类型
        ExtendReadingResponse urlResp = readingList.stream()
                .filter(item -> item.getId().equals(urlReadingId))
                .findFirst()
                .orElse(null);
        Assertions.assertEquals("URL", urlResp.getMaterialType());
        Assertions.assertEquals("https://docs.spring.io/spring-boot/docs/current/reference/html/", urlResp.getUrl());

        // 校验TEXT类型
        ExtendReadingResponse textResp = readingList.stream()
                .filter(item -> item.getId().equals(textReadingId))
                .findFirst()
                .orElse(null);
        Assertions.assertEquals("TEXT", textResp.getMaterialType());
        Assertions.assertEquals("@SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan", textResp.getContent());

        // 校验FILE类型
        ExtendReadingResponse fileResp = readingList.stream()
                .filter(item -> item.getId().equals(fileReadingId))
                .findFirst()
                .orElse(null);
        Assertions.assertEquals("FILE", fileResp.getMaterialType());
        Assertions.assertEquals("PDF", fileResp.getFileType());
        Assertions.assertEquals(1024000L, fileResp.getFileSize());

        // ========== 删除拓展阅读 ==========
        boolean deleteSuccess = extendReadingService.deleteExtendReading(urlReadingId, STUDENT_ID);
        Assertions.assertTrue(deleteSuccess, "拓展阅读删除失败");
        // 校验删除后不存在
        List<ExtendReadingResponse> afterDeleteList = extendReadingService.getExtendReadingList(
                STUDENT_ID, TEXTBOOK_ID, CHAPTER_ID);
        Assertions.assertFalse(afterDeleteList.stream().anyMatch(item -> item.getId().equals(urlReadingId)));

        System.out.println("拓展阅读测试通过，URL类型ID：" + urlReadingId + "（已删除），TEXT类型ID：" + textReadingId + "，FILE类型ID：" + fileReadingId);
    }
}
