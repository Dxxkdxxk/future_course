package com.lzlz.springboot.security;

import com.lzlz.springboot.security.config.WebSocketConfig;
import com.lzlz.springboot.security.entity.Chapter;
import com.lzlz.springboot.security.entity.Textbook;
import com.lzlz.springboot.security.mapper.ChapterMapper;
import com.lzlz.springboot.security.mapper.TextbookMapper;
import com.lzlz.springboot.security.service.DocumentParseService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Slf4j
@TestConfiguration
@ComponentScan(
        basePackages = "com.lzlz.springboot.security", // 你的业务包
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = WebSocketConfig.class // 排除WebSocket配置类
                )
        }
)
@ActiveProfiles("test") // 激活test环境，WebSocketConfig自动排除
public class DocumentParseTest {

    @Mock
    private ChapterMapper chapterMapper;

//    @Mock
//    private WebSocketConfig.ParseCallbackHandler webSocketHandler;

    @InjectMocks
    private DocumentParseService documentParseService;

    private Textbook textbook;

    @Mock
    private TextbookMapper textbookMapper;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 初始化测试用教材信息
        textbook = new Textbook();
        textbook.setId(1L);
        textbook.setName("Test Textbook");
        textbook.setFileType("PDF"); // 或 "WORD"
        textbook.setMinioBucket("test-bucket");
        textbook.setMinioObjectName("test.pdf");

        // 模拟数据库查询返回教材
        when(textbookMapper.selectById(1L)).thenReturn(textbook);
    }

    @Test
    void testParseLocalPdf() throws Exception {
        // 本地测试 PDF 文件路径（请替换为你的实际文件路径）
        String localPdfPath = "G:\\doc-CH1.docx";
        log.info("开始测试本地PDF解析，文件路径：{}", localPdfPath);

        // 执行本地文件分节
        documentParseService.parseLocalFile(localPdfPath, 1L, 1001L);

//        // 手动打印解析结果（辅助调试）
//        List<Chapter> chapters = chapterMapper.selectByTextbookId(1L); // 新增查询方法
//        log.info("测试完成，实际插入章节数：{}", chapters.size());

        // 验证结果
        verify(chapterMapper, atLeastOnce()).batchInsert(anyList()); // 允许多次调用（兼容空列表）
    }

    @Test
    void testParseLocalWord() throws Exception {
        // 本地测试 Word 文件路径（请替换为你的实际文件路径）
        String localDocxPath = "G:\\doc-CH1.docx";

        // 修改教材类型为 WORD
        textbook.setFileType("WORD");

        // 执行本地文件分节
        documentParseService.parseLocalFile(localDocxPath, 1L, 1001L);

        // 验证结果
        verify(chapterMapper).batchInsert(anyList());
//        verify(webSocketHandler).pushParseResult(eq(1001L), argThat(result ->
//                result.getStatus().equals(Textbook.STATUS_SUCCESS)
//        ));
    }

    @Test
    void testParseLocalInvalidFile() throws Exception {
        // 本地无效文件路径
        String invalidFilePath = "src/test/resources/test.txt";

        // 修改教材类型为无效类型
        textbook.setFileType("TXT");

        // 执行本地文件分节
        documentParseService.parseLocalFile(invalidFilePath, 1L, 1001L);

        // 验证失败处理
//        verify(webSocketHandler).pushParseResult(eq(1001L), argThat(result ->
//                result.getStatus().equals(Textbook.STATUS_FAIL)
//        ));
    }
}