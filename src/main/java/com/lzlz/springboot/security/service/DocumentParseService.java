package com.lzlz.springboot.security.service;

import org.apache.poi.xwpf.usermodel.XWPFRun;
import com.lzlz.springboot.security.domain.ParseCallbackHandler;
import com.lzlz.springboot.security.entity.Chapter;
import com.lzlz.springboot.security.entity.Textbook;
import com.lzlz.springboot.security.mapper.ChapterMapper;
import com.lzlz.springboot.security.mapper.TextbookMapper;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DocumentParseService {
    @Autowired
    private TextbookMapper textbookMapper;
    @Autowired
    private ChapterMapper chapterMapper;
    @Autowired
    private ParseCallbackHandler webSocketHandler;

    private static final Set<Character> ZHANG_CONFUSIONS =
            new HashSet<>(Arrays.asList('章', '虱', '童', '掌', '障', '彰'));

    /**
     * 解析本地文件（用于测试）
     * @param localFilePath 本地文件路径
     * @param textbookId 教材 ID
     * @param uploaderId 上传者 ID
     */
    public void parseLocalFile(String localFilePath, Long textbookId, Long uploaderId) {
        Textbook textbook = textbookMapper.selectById(textbookId);
        if (textbook == null) {
            log.error("教材不存在：textbookId={}", textbookId);
//            pushFailResult(uploaderId, textbookId, "教材不存在");
            return;
        }

        try {
            // 更新教材状态为“解析中”
            textbook.setStatus(Textbook.STATUS_PARSING);
            textbookMapper.updateById(textbook);

            // 直接读取本地文件
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new FileNotFoundException("本地文件不存在：" + localFilePath);
            }

            // 根据文件类型解析章节
            List<Chapter> chapters = new ArrayList<>();
            String fileType = textbook.getFileType();
            if ("WORD".equalsIgnoreCase(fileType)) {
                chapters = parseWordChapters(file, textbookId);
            } else if ("PDF".equalsIgnoreCase(fileType)) {
                chapters = parsePdfChapters(file, textbookId);
            } else {
                throw new IllegalArgumentException("不支持的文件类型：" + fileType);
            }

            // 批量保存章节到 MySQL
            if (!chapters.isEmpty()) {
                chapterMapper.batchInsert(chapters);
            }

            // 更新教材状态为“解析成功”
            textbook.setStatus(Textbook.STATUS_SUCCESS);
            textbookMapper.updateById(textbook);

            // WebSocket 回调前端：解析成功
//            pushSuccessResult(uploaderId, textbook, chapters.size());

        } catch (Exception e) {
            log.error("文档分节失败：textbookId={}", textbookId, e);
            // 更新教材状态为“解析失败”
            textbook.setStatus(Textbook.STATUS_FAIL);
            textbookMapper.updateById(textbook);
            // 回调前端：解析失败
//            pushFailResult(uploaderId, textbookId, e.getMessage());
        }
    }
    /**
     * 异步解析文档分节（@Async标识异步任务）
     * @param textbookId 教材ID
     */
    @Async
    public void asyncParseChapter(Long textbookId) {
        Textbook textbook = textbookMapper.selectById(textbookId);
        if (textbook == null) {
            log.error("教材不存在：textbookId={}", textbookId);
//            pushFailResult(uploaderId, textbookId, "教材不存在");
            return;
        }

        try {
            // 1. 更新教材状态为“解析中”
            textbook.setStatus(Textbook.STATUS_PARSING);
            textbookMapper.updateById(textbook);

            // 2. 下载MinIO文件到本地临时目录（解析需要文件流）
            File tempFile = downloadMinIOFile(textbook.getMinioBucket(), textbook.getMinioObjectName());

            if (tempFile != null)
                log.info("临时文件路径:{}", tempFile.getAbsolutePath());
                
            // 3. 根据文件类型解析章节
            List<Chapter> chapters = new ArrayList<>();
            if ("WORD".equals(textbook.getFileType())) {
                chapters = parseWordChapters(tempFile, textbookId);
            } else if ("PDF".equals(textbook.getFileType())) {
                chapters = parsePdfChapters(tempFile, textbookId);
            }

            // 4. 批量保存章节到MySQL
            if (!chapters.isEmpty()) {
                chapterMapper.batchInsert(chapters);
            }

            // 5. 更新教材状态为“解析成功”
            textbook.setStatus(Textbook.STATUS_SUCCESS);
            textbookMapper.updateById(textbook);

            // 6. WebSocket回调前端：解析成功
//            pushSuccessResult(uploaderId, textbook, chapters.size());

        } catch (Exception e) {
            log.error("文档分节失败：textbookId={}", textbookId, e);
            // 更新教材状态为“解析失败”
            textbook.setStatus(Textbook.STATUS_FAIL);
            textbookMapper.updateById(textbook);
            // 回调前端：解析失败
//            pushFailResult(uploaderId, textbookId, e.getMessage());
        } finally {
            // 删除临时文件（避免磁盘占用）
            File tempFile = new File(System.getProperty("java.io.tmpdir") + File.separator + textbook.getMinioObjectName());
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // 标题字号阈值（大于等于此值判定为标题，可根据实际需求调整）
    private static final int TITLE_FONT_SIZE_THRESHOLD = 14;
    // 正则匹配规则
    // 一级标题：第1章/第一章 开头
    private static final Pattern LEVEL1_PATTERN = Pattern.compile("^第[一二三四五六七八九十0-9]+章.*");
    // 二级标题：1.1/一.一 开头
    private static final Pattern LEVEL2_PATTERN = Pattern.compile("^[一二三四五六七八九十0-9]+\\.[一二三四五六七八九十0-9]+.*");
    // 三级标题：1.1.1/一.一.一 开头
    private static final Pattern LEVEL3_PATTERN = Pattern.compile("^[一二三四五六七八九十0-9]+\\.[一二三四五六七八九十0-9]+\\.[一二三四五六七八九十0-9]+.*");

    /**
     * 解析Word文档章节（支持章、节、小节三级，字号过滤）
     */
public List<Chapter> parseWordChapters(File wordFile, Long textbookId) throws Exception {
    List<Chapter> chapters = new ArrayList<>();
    try (XWPFDocument document = new XWPFDocument(new FileInputStream(wordFile))) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        log.info("textbookId={}, 段落总数={}", textbookId, paragraphs.size());

        Chapter currentChapter = null;
        Chapter currentSection = null;
        int chapterSort = 1;
        int sectionSort = 1;
        int subSectionSort = 1;

        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph paragraph = paragraphs.get(i);
            String rawText = paragraph.getText();
            String text = rawText == null ? "" : rawText.trim();

            String style = paragraph.getStyle();

            Integer fontSize = null;
            if (paragraph.getRuns() != null && !paragraph.getRuns().isEmpty()) {
                XWPFRun run = paragraph.getRuns().get(0);
                fontSize = run.getFontSize();
            }

            log.info("段落[{}]: text='{}', style='{}', fontSize={}", i, text, style, fontSize);

            if (text.isEmpty()) {
                continue;
            }

            boolean titleParagraph = isTitleParagraph(paragraph);
            log.info("段落[{}] 是否标题: {}", i, titleParagraph);
            if (!titleParagraph) {
                continue;
            }

            text = text.replaceAll("\\s+", "");
            log.info("段落[{}] 清洗后 text='{}'", i, text);

            boolean level1 = LEVEL1_PATTERN.matcher(text).matches();
            boolean level2 = LEVEL2_PATTERN.matcher(text).matches();
            boolean level3 = LEVEL3_PATTERN.matcher(text).matches();

            log.info("段落[{}] 正则匹配: level1={}, level2={}, level3={}", i, level1, level2, level3);

            if ("Heading 1".equals(style) || level1) {
                Chapter chapter = createChapter(textbookId, null, 1, text, chapterSort++);
                chapters.add(chapter);
                currentChapter = chapter;
                currentSection = null;
                sectionSort = 1;
                subSectionSort = 1;
            } else if ("Heading 2".equals(style) || level2) {
                if (currentChapter == null) {
                    log.warn("解析到二级标题但无关联一级标题，文本：{}", text);
                    continue;
                }
                Chapter section = createChapter(textbookId, currentChapter.getId(), 2, text, sectionSort++);
                chapters.add(section);
                currentSection = section;
                subSectionSort = 1;
            } else if ("Heading 3".equals(style) || level3) {
                if (currentSection == null) {
                    log.warn("解析到三级标题但无关联二级标题，文本：{}", text);
                    continue;
                }
                Chapter subSection = createChapter(textbookId, currentSection.getId(), 3, text, subSectionSort++);
                chapters.add(subSection);
            }
        }
    } catch (Exception e) {
        log.error("解析Word章节失败，textbookId={}", textbookId, e);
        throw e;
    }

    log.info("Word文档解析完成：textbookId={}，解析出章节总数={}", textbookId, chapters.size());
    return chapters;
}

    /**
     * 构建章节对象
     */
    private Chapter createChapter(Long textbookId, Long parentId, Integer level, String title, Integer sort) {
        Chapter chapter = new Chapter();
        chapter.setTextbookId(textbookId);
        chapter.setParentId(parentId);
        chapter.setLevel(level);
        chapter.setTitle(title);
        chapter.setSort(sort);
        chapter.setCreateTime(LocalDateTime.now());
        return chapter;
    }

    /**
     * 判断段落是否为标题（通过字号阈值）
     */
 private boolean isTitleParagraph(XWPFParagraph paragraph) {
    String text = paragraph.getText().trim();
    if (text.isEmpty()) {
        return false;
    }

    // 清理空格，方便正则匹配
    String cleanText = text.replaceAll("\\s+", "");

    // ==========================================
    // 维度 1：强特征匹配（最高优先级）
    // 如果文本以 "第X章"、"1."、"1.1"、"1.1.1" 开头，直接认定为标题，绕过字号检查！
    // ==========================================
    String titleRegex = "^(第[一二三四五六七八九十百]+[章|节]|\\d+\\.\\d+\\.\\d+|\\d+\\.\\d+|\\d+\\.).*";
    if (cleanText.matches(titleRegex)) {
        log.info("【正则命中】识别为标题: {}", text);
        return true;
    }

    // ==========================================
    // 维度 2：Word 底层大纲级别检查
    // 即使用户没有设置样式，有时 Word 也会自动关联大纲级别 (Outline Level)
    // ==========================================
    if (paragraph.getCTP() != null && paragraph.getCTP().getPPr() != null) {
        if (paragraph.getCTP().getPPr().getOutlineLvl() != null) {
            log.info("【大纲级别命中】识别为标题: {}", text);
            return true;
        }
    }

    // ==========================================
    // 维度 3：安全的字号与加粗检查 (兜底逻辑)
    // ==========================================
    double maxFontSize = -1;
    boolean isBold = false;
    
    for (XWPFRun run : paragraph.getRuns()) {
        // 安全获取字号，避免 NPE
        Double fontSize = run.getFontSizeAsDouble();
        if (fontSize != null && fontSize > maxFontSize) {
            maxFontSize = fontSize;
        }
        if (run.isBold()) {
            isBold = true;
        }
    }

    // 假设：字号大于 14pt (约四号字) 或者 (字号未明确提取到但整段加粗且长度较短) 认为是标题
    if (maxFontSize >= 14.0) {
        log.info("【字号命中】字号为 {}, 识别为标题: {}", maxFontSize, text);
        return true;
    }
    
    // 如果连字号都拿不到(-1)，但文本很短（比如小于30个字）且被加粗了，也大概率是标题
    if (maxFontSize == -1 && isBold && text.length() < 30) {
        log.info("【短文本加粗命中】识别为标题: {}", text);
        return true;
    }

    return false;
}
    private List<Chapter> parsePdfChapters(File pdfFile, Long textbookId) throws Exception {
        final List<Chapter> chapters = new ArrayList<>();
        log.info("进入方法parsePdfChapters，文件路径：{}，文件大小：{}KB",
                pdfFile.getAbsolutePath(), pdfFile.length() / 1024);
        AtomicBoolean isTocPageRef = new AtomicBoolean(false);
        List<String> tocKeywords = Arrays.asList("目录", "TOC", "CONTENTS", "目次");
        AtomicReference<Float> pageWidthRef = new AtomicReference<>(0.0f);
        AtomicReference<String> currentPageText = new AtomicReference<>("");
        AtomicInteger currentProcessingPageNum = new AtomicInteger(0);

        try (PDDocument document = PDDocument.load(pdfFile)) {
            // 1. 检测是否加密
            if (document.isEncrypted()) {
                log.warn("PDF文件已加密，尝试解密（默认空密码）");
                // 补充：实际场景需处理加密PDF的解密逻辑，此处仅日志提示
            }

            // 2. 抽样检测是否有文本层
            int totalPages = document.getNumberOfPages(); // 提前获取总页数，避免后续重复调用
            boolean hasTextContent = false;
            PDFTextStripper tempStripper = new PDFTextStripper();
            tempStripper.setSortByPosition(true);
            int samplePages = Math.min(totalPages, 5);
            for (int i = 1; i <= samplePages; i++) {
                tempStripper.setStartPage(i);
                tempStripper.setEndPage(i);
                String pageText = tempStripper.getText(document).trim();
                if (!pageText.isEmpty()) {
                    hasTextContent = true;
                    log.info("[抽样页{}] 提取到文本（长度：{}）：{}",
                            i, pageText.length(), pageText.substring(0, Math.min(pageText.length(), 100)));
                    break;
                }
            }

            // 3. 扫描件分支（核心改造：替换原有简单OCR逻辑）
            if (!hasTextContent) {
                log.warn("PDF无文本层，启用【增强版OCR+智能章节提取】");
                chapters.clear();
                chapters.addAll(parseScannedPdfChapters(pdfFile, textbookId));
                return chapters;
            }

            // ========== 移除错误的document重新赋值代码 ==========
            // 原错误代码：if (document == null || document.isClosed()) { ... }

            // 4. 原有有文本层PDF的解析逻辑（修复内部类初始化问题）
            PDFTextStripper stripper = new PDFTextStripper();
            // 显式设置页码（替代初始化块中的直接调用）
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(totalPages); // 使用提前获取的totalPages，避免依赖document
            stripper.setLineSeparator(System.lineSeparator());

            // 重写writeString方法（分离初始化和方法重写，避免初始化块依赖document）
            stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                    super.writeString(text, textPositions);
                    int currentPage = getCurrentPageNo();
                    currentProcessingPageNum.set(currentPage);

                    String rawText = text;
                    String trimmedText = rawText.trim();
                    log.debug("[页{}] 原始文本（长度：{}）：【{}】", currentPage, rawText.length(),
                            rawText.replaceAll("\\s+", "□"));

                    if (rawText.isEmpty()) {
                        log.warn("[页{}] 提取到空文本段，跳过", currentPage);
                        return;
                    }

                    currentPageText.set(currentPageText.get() + rawText);

                    if (pageWidthRef.get() == 0) {
                        // 增加空值校验，避免NPE
                        PDPage page = document.getPage(currentPage - 1);
                        if (page == null) {
                            log.warn("[页{}] 页面对象为空，跳过宽度计算", currentPage);
                            return;
                        }
                        float pageWidth = page.getCropBox() != null ? page.getCropBox().getWidth() : page.getMediaBox().getWidth();
                        pageWidthRef.set(pageWidth);
                        log.info("[页{}] 页面宽度：{}px", currentPage, pageWidth);
                    }
                    float pageWidth = pageWidthRef.get();
                    TextPosition firstPos = textPositions.get(0);
                    float fontSize = firstPos.getFontSizeInPt();
                    float textCenterX = firstPos.getX() + firstPos.getWidth() / 2;
                    float pageCenterX = pageWidth / 2;
                    boolean isCentered = Math.abs(textCenterX - pageCenterX) <= 20;

                    if (isTocPageRef.get()) {
                        log.warn("[页{}] 文本【{}】被过滤：目录页", currentPage, trimmedText);
                        return;
                    }
                    boolean isTocTitle = tocKeywords.stream().anyMatch(trimmedText::contains)
                            && fontSize >= 18 && isCentered;
                    if (isTocTitle) {
                        isTocPageRef.set(true);
                        log.info("[页{}] 标记为目录页：{}", currentPage, trimmedText);
                        return;
                    }
                    if (isTocLine(trimmedText)) {
                        log.warn("[页{}] 文本【{}】被过滤：目录行", currentPage, trimmedText);
                        return;
                    }

                    if (trimmedText.length() >= 2) {
                        if (trimmedText.contains("章")) {
                            addChapter(chapters, textbookId, null, 1, trimmedText, currentPage);
                        } else if (trimmedText.matches("^\\d+\\..*")) {
                            Chapter parent = getLastChapterByLevel(chapters, 1);
                            if (parent != null) {
                                addChapter(chapters, textbookId, parent.getId(), 2, trimmedText, currentPage);
                            }
                        }
                    }
                }

                @Override
                protected void endPage(PDPage page) throws IOException {
                    super.endPage(page);
                    int pageNum = document.getPages().indexOf(page) + 1;
                    String fullPageText = currentPageText.get();
                    log.info("[页{}] 整页文本（总长度：{}）：【{}】",
                            pageNum, fullPageText.length(),
                            fullPageText.isEmpty() ? "空" : fullPageText.substring(0, Math.min(fullPageText.length(), 200)));
                    isTocPageRef.set(false);
                    currentPageText.set("");
                    log.info("[页{}] 解析完成，已识别章节数：{}", pageNum, chapters.size());
                }

                private boolean isTocLine(String text) {
                    return text.matches(".*[\\s-]{2,}\\d+$")
                            && !text.contains("章")
                            && !text.contains("节");
                }

                private Chapter getLastChapterByLevel(List<Chapter> chapters, int level) {
                    return chapters.stream()
                            .filter(c -> c.getLevel() == level)
                            .reduce((a, b) -> b)
                            .orElse(null);
                }
            };

            log.info("开始提取PDF文本，总页数：{}", totalPages);
            String allText = stripper.getText(document);
            log.info("PDF全文本提取完成（总长度：{}），前500字符：{}",
                    allText.length(), allText.substring(0, Math.min(allText.length(), 500)));

        } catch (Exception e) {
            log.error("PDF解析异常", e);
            throw e;
        }
        log.info("PDF解析完成：textbookId={}，最终章节数={}", textbookId, chapters.size());
        return chapters;
    }
    /**
     * 增强版PDF转图片（含图像预处理：降噪、二值化、提升对比度）
     */
// 改造pdfToEnhancedImages方法：给图片添加DPI信息
    private List<BufferedImage> pdfToEnhancedImages(File pdfFile) throws Exception {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int dpi = 400; // 明确设置DPI
            float scale = dpi / 72f; // PDF默认72DPI，转换为目标DPI的缩放因子

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                long start = System.currentTimeMillis();
                log.info("开始处理第{}页PDF转图片（DPI={}）", i+1, dpi);
                // 核心修改：按缩放因子渲染，生成带DPI的图片
                BufferedImage rawImage = renderer.renderImage(i, scale, ImageType.RGB);

                // 手动给图片添加DPI元数据（关键：解决Tesseract DPI警告）
                setImageDPI(rawImage, dpi);

                log.info("第{}页PDF转图片完成，尺寸={}×{}，耗时{}ms",
                        i+1, rawImage.getWidth(), rawImage.getHeight(), System.currentTimeMillis()-start);

                // 即使禁用OpenCV，也要做基础图像增强
                BufferedImage enhancedImage = simpleImageEnhance(rawImage);
                images.add(enhancedImage);

//                if (i >= 1) break; // 仅处理前2页
            }
        }
        return images;
    }

    // 新增：给BufferedImage设置DPI元数据
    private void setImageDPI(BufferedImage image, int dpi) throws IIOInvalidTreeException {
        // 设置图片分辨率（DPI）
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        IIOMetadata metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), param);

        // 写入DPI元数据
        double dotsPerMeter = dpi * 39.3701; // 英寸转米（1英寸=39.3701厘米）
        metadata.mergeTree("javax_imageio_1.0", new IIOMetadataNode("javax_imageio_1.0") {{
            appendChild(new IIOMetadataNode("HorizontalPixelSize") {{
                setNodeValue(String.valueOf(1/dotsPerMeter));
            }});
            appendChild(new IIOMetadataNode("VerticalPixelSize") {{
                setNodeValue(String.valueOf(1/dotsPerMeter));
            }});
        }});
    }

    // 新增：简易图像增强（无OpenCV时替代）
    private BufferedImage simpleImageEnhance(BufferedImage image) {
        // 1. 转为灰度图
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // 2. 手动二值化（提升对比度，解决扫描件模糊）
        BufferedImage binaryImage = new BufferedImage(grayImage.getWidth(), grayImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2 = binaryImage.createGraphics();
        g2.drawImage(grayImage, 0, 0, null);
        g2.dispose();

        return binaryImage;
    }

    /**
     * 图像增强：降噪 + 二值化 + 提升对比度（解决扫描件模糊、噪点问题）
     */
    private BufferedImage enhanceImage(BufferedImage rawImage) {
        // 1. BufferedImage转OpenCV的Mat
        Mat mat = bufferedImageToMat(rawImage);
        // 2. 转为灰度图
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        // 3. 降噪（中值滤波，去除扫描件的斑点噪点）
        Imgproc.medianBlur(mat, mat, 3);
        // 4. 自适应二值化（解决扫描件明暗不均）
        Imgproc.adaptiveThreshold(mat, mat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 15, 8);
        // 5. 提升对比度（可选）
//        Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX);
        // 6. Mat转回BufferedImage
        return matToBufferedImage(mat);
    }

    /**
     * 优化Tesseract配置，提升中文+数字识别精度
     */
    private ITesseract initTesseract() {
        Tesseract tesseract = new Tesseract();
        // 1. 设置训练数据路径（确保chi_sim.traineddata存在）
        tesseract.setDatapath("G:\\Tesseract-OCR\\tessdata");
        // 2. 启用中文+数字混合识别（chi_sim+eng）
        tesseract.setLanguage("chi_sim+eng");
        // 显式启用HOCR输出（5.5.0需手动设置）
        tesseract.setTessVariable("tessedit_create_hocr", "1");
        // 分页模式：自动检测文本块（适合扫描件）
        tesseract.setTessVariable("tessedit_pageseg_mode", "3");
        // 启用旧引擎（提升中文HOCR生成率）
        tesseract.setTessVariable("user_defined_dpi", "300");
        tesseract.setTessVariable("tessedit_ocr_engine_mode", "1");
        return tesseract;
    }

    /**
     * 写入Tesseract配置文件（5.x 必须通过配置文件设置参数）
     */
    private void writeTesseractConfigFile() {
        // 配置文件路径（项目根目录下的tessconfigs/myconfig）
        File configDir = new File("tessconfigs");
        if (!configDir.exists()) configDir.mkdirs();
        File configFile = new File(configDir, "myconfig");

        try (PrintWriter writer = new PrintWriter(configFile)) {
            // 白名单：只识别标题相关字符
            writer.println("tessedit_char_whitelist 0123456789.一二三四五六七八九十百千万章节节()（）、，.： ");
            // 页面分割模式：按行识别
            writer.println("tessedit_pageseg_mode 6");
            // 关闭多余空格
            writer.println("preserve_interword_spaces 0");
        } catch (IOException e) {
            log.error("写入Tesseract配置文件失败", e);
        }
    }

    /**
     * OCR识别（含排版特征提取）
     */
    private List<OcrLine> extractOcrLinesWithLayout(File pdfFile) throws Exception {
        List<OcrLine> ocrLines = new ArrayList<>();
        ITesseract tesseract = initTesseract();
        List<BufferedImage> images = pdfToEnhancedImages(pdfFile);
        int totalImages = images.size();
        log.info("开始OCR识别：总图片数={}", totalImages);

        for (int pageNum = 0; pageNum < totalImages; pageNum++) {
            long start = System.currentTimeMillis();
            BufferedImage image = images.get(pageNum);
            log.info("开始OCR识别第{}页图片（宽={},高={}）", pageNum+1, image.getWidth(), image.getHeight());

            try {
                log.info("开始doOCR");
                // 核心卡点：getHOCRText可能阻塞
                String hocr = tesseract.doOCR(image);
                log.info("第{}页HOCR文本提取完成，长度={}，耗时{}ms",
                        pageNum+1, hocr.length(), System.currentTimeMillis()-start);

                start = System.currentTimeMillis();
                log.info("准备开始parse");
                List<OcrLine> pageLines = parseHocrToLines(hocr, pageNum + 1);
                log.info("第{}页HOCR解析完成，提取{}行文本，耗时{}ms",
                        pageNum+1, pageLines.size(), System.currentTimeMillis()-start);

                ocrLines.addAll(pageLines);
            } catch (TesseractException e) {
                log.error("[OCR-页{}] 识别失败，耗时{}ms", pageNum + 1, System.currentTimeMillis()-start, e);
            }
        }
        log.info("OCR识别完成：共提取{}行文本", ocrLines.size());
        return ocrLines;
    }

    /**
     * 智能提取章节（核心方法）
     */
    private List<Chapter> extractChaptersFromOcrLines(List<OcrLine> ocrLines, Long textbookId) {
        List<Chapter> chapters = new ArrayList<>();
        Chapter lastLevel1Chapter = null; // 最近的一级章节（章）
        Chapter lastLevel2Chapter = null; // 最近的二级章节（节）

        // 定义标题格式的增强正则（兼容OCR识别误差）
        // 一级标题：第X章 XXX（支持中文/数字章号，兼容OCR断字）
        Pattern level1Pattern = Pattern.compile("^第[一二三四五六七八九十0-9]+章\\s+.{1,20}$");
        // 二级标题：X.X XXX（支持1.1、1 .1、1，1等OCR错误格式）
        Pattern level2Pattern = Pattern.compile("^\\d+[\\.，、]\\d+\\s+.{1,20}$");
        // 三级标题：X.X.X XXX
        Pattern level3Pattern = Pattern.compile("^\\d+[\\.，、]\\d+[\\.，、]\\d+\\s+.{1,20}$");

        for (int i = 0; i < ocrLines.size(); i++) {
            OcrLine line = ocrLines.get(i);
            String text = line.getText();
            // 跳过过短/过长文本（标题长度通常1-20字）
            if (text.length() < 2 || text.length() > 20) continue;

            // 1. 判断是否为一级标题（章）：格式匹配 + 排版特征（居中/顶部+大字号）
            boolean isLevel1 = level1Pattern.matcher(text).matches()
                    && (line.isTopArea())
                    && line.getFontSize() >= 16; // 标题字号≥16px

            // 2. 判断是否为二级标题（节）：格式匹配 + 排版特征（顶部+中字号）
            boolean isLevel2 = level2Pattern.matcher(text).matches()
                    && line.getFontSize() >= 12;

            // 3. 判断是否为三级标题（小节）
            boolean isLevel3 = level3Pattern.matcher(text).matches()
                    && line.getFontSize() >= 10;

            // 4. 上下文过滤：排除正文里的“章”（比如前后行是正文内容）
            if (isLevel1 || isLevel2 || isLevel3) {
                boolean isRealTitle = checkContextIsTitle(ocrLines, i);
                if (!isRealTitle) {
                    log.warn("过滤误识别标题：{}（上下文不符合标题特征）", text);
                    continue;
                }
            }

            // 5. 构建章节
            if (isLevel1) {
                lastLevel1Chapter = buildChapter(textbookId, null, 1, text, line.getPageNum(), chapters);
                lastLevel2Chapter = null; // 重置二级章节
                log.info("识别一级章节：{}", text);
            } else if (isLevel2 && lastLevel1Chapter != null) {
                lastLevel2Chapter = buildChapter(textbookId, lastLevel1Chapter.getId(), 2, text, line.getPageNum(), chapters);
                log.info("识别二级章节：{}（父章：{}）", text, lastLevel1Chapter.getTitle());
            } else if (isLevel3 && lastLevel2Chapter != null) {
                buildChapter(textbookId, lastLevel2Chapter.getId(), 3, text, line.getPageNum(), chapters);
                log.info("识别三级章节：{}（父节：{}）", text, lastLevel2Chapter.getTitle());
            }
        }
        return chapters;
    }

    private List<Chapter> parseScannedPdfChapters(File pdfFile, Long textbookId) throws Exception {
        // ========== 核心修改：手动加载OpenCV本地库 ==========
            // 方式1：指定绝对路径（推荐，避免环境变量问题）
        System.load("G:\\GraduationDesign\\bge-base-zh-v1.5\\opencv\\build\\java\\x64\\opencv_java490.dll");
        // 方式2：若配置了环境变量，可使用（备选）
        // System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        log.info("OpenCV本地库加载成功");

        // 原有逻辑不变
        List<OcrLine> ocrLines = extractOcrLinesWithLayout(pdfFile);
        log.info("ocr长度：{}", ocrLines.size());
        List<Chapter> chapters = extractChaptersFromOcrLines(ocrLines, textbookId);
        log.info("扫描件PDF增强版解析完成：共识别{}个章节", chapters.size());
        return chapters;
    }

    /**
     * 上下文校验：判断当前行是否为真实标题（排除正文里的零散匹配）
     */
    private boolean checkContextIsTitle(List<OcrLine> ocrLines, int currentIndex) {
        OcrLine currentLine = ocrLines.get(currentIndex);
        // 规则1：标题行前后大概率是空行/短行（正文行是连续长文本）
        int prevIndex = currentIndex - 1;
        int nextIndex = currentIndex + 1;
        boolean prevIsEmpty = prevIndex < 0 || ocrLines.get(prevIndex).getText().length() < 2;
        boolean nextIsEmpty = nextIndex >= ocrLines.size() || ocrLines.get(nextIndex).getText().length() < 2;

        // 规则2：标题行不会包含“本章”“本节”“什么”“哪些”等正文关键词
        String[] excludeKeywords = {"本章", "本节", "什么", "哪些", "区别", "介绍", "分析", "说明", "问题"};
        boolean containsExcludeWord = Arrays.stream(excludeKeywords).anyMatch(currentLine.getText()::contains);

        // 规则3：标题行字号显著大于前后行（如果有前后行）
        boolean fontSizeLarger = true;
        if (prevIndex >= 0) {
            fontSizeLarger = currentLine.getFontSize() > ocrLines.get(prevIndex).getFontSize() + 2;
        }
        if (nextIndex < ocrLines.size() && fontSizeLarger) {
            fontSizeLarger = currentLine.getFontSize() > ocrLines.get(nextIndex).getFontSize() + 2;
        }

        // 综合判断：（前后空行 OR 字号更大） AND 不包含排除关键词
        return (prevIsEmpty || nextIsEmpty || fontSizeLarger) && !containsExcludeWord;
    }

    /**
     * 构建章节对象（自动计算排序号）
     */
    private Chapter buildChapter(Long textbookId, Long parentId, int level, String title, int pageNum, List<Chapter> chapters) {
        // 去重+排序
        long sameParentCount = chapters.stream().filter(c -> Objects.equals(c.getParentId(), parentId)).count();
        Chapter chapter = new Chapter();
        chapter.setTextbookId(textbookId);
        chapter.setParentId(parentId);
        chapter.setLevel(level);
        chapter.setTitle(title);
        chapter.setSort((int) sameParentCount + 1);
        chapter.setPdfPage(pageNum);
        chapter.setCreateTime(LocalDateTime.now());
        chapters.add(chapter);
        return chapter;
    }

    // 封装OCR行的排版特征
    @Data
    static class OcrLine {
        private int pageNum; // 页码
        private String text; // 清洗后的文本
        private int x1, y1, x2, y2; // 文本行坐标
        private int width; // 行宽度
        private int height; // 行高度
        private int fontSize; // 估算字号
        private boolean centered; // 是否居中
        private boolean topArea; // 是否在页面顶部
    }

    /**
     * 解析HOCR数据，提取每行的文本+排版特征
     * HOCR是Tesseract输出的HTML格式，包含每行的bbox（坐标）、字号等信息
     */
    private List<OcrLine> parseHocrToLines(String hocr, int pageNum) {
        List<OcrLine> lines = new ArrayList<>();
        log.info("parse开始");

        // ========== 1. 空HOCR降级处理 ==========
        if (hocr == null || hocr.trim().isEmpty()) {
            log.warn("HOCR文本为空，跳过解析");
            return lines;
        }

        // ========== 2. 打印原始HOCR（调试） ==========
        String debugHocr = hocr.substring(0, Math.min(hocr.length(), 500));
        log.info("原始HOCR（前500字符）：\n{}", debugHocr);

        try {
            // ========== 3. Jsoup解析HOCR（兼容HTML格式） ==========
            Document doc = Jsoup.parse(hocr);
            // 匹配所有ocr_line标签（兼容单/双引号、空格）
            Elements ocrLines = doc.select("div.ocr_line");
            if (ocrLines.isEmpty()) {
                ocrLines = doc.select("span.ocr_line");
                if (ocrLines.isEmpty()) {
                    ocrLines = doc.select("p.ocr_par");
                }
            }
            log.info("找到{}个ocr_line标签", ocrLines.size());

            if (ocrLines.isEmpty()) {
                log.warn("未找到ocr_line标签，尝试纯文本提取");
                // 降级：从HOCR中提取所有文本（无排版特征）
                String plainText = doc.text();
                String cleanText = cleanOcrText(plainText);
                if (!cleanText.isEmpty()) {
                    OcrLine fallbackLine = new OcrLine();
                    fallbackLine.setPageNum(pageNum);
                    fallbackLine.setText(cleanText);
                    lines.add(fallbackLine);
                }
                return lines;
            }

            // ========== 4. 解析每个ocr_line的排版特征 ==========
            // 从ocr_page获取页面尺寸（兼容不同DPI的图片）
            Element ocrPage = doc.selectFirst("div.ocr_page");
            int pageWidth = 2439; // 默认值（你的图片宽度）
            int pageHeight = 3236; // 默认值（你的图片高度）
            if (ocrPage != null) {
                String pageTitle = ocrPage.attr("title");
                // 提取页面bbox：bbox 0 0 2439 3236
                Pattern pageBboxPattern = Pattern.compile("bbox (\\d+) (\\d+) (\\d+) (\\d+)");
                Matcher pageMatcher = pageBboxPattern.matcher(pageTitle);
                if (pageMatcher.find()) {
                    pageWidth = Integer.parseInt(pageMatcher.group(3));
                    pageHeight = Integer.parseInt(pageMatcher.group(4));
                    log.info("从HOCR获取页面尺寸：{}×{}", pageWidth, pageHeight);
                }
            }

            for (Element lineElem : ocrLines) {
                // 提取行的bbox（核心：从title属性解析）
                String lineTitle = lineElem.attr("title");
                Pattern bboxPattern = Pattern.compile("bbox (\\d+) (\\d+) (\\d+) (\\d+)");
                Matcher bboxMatcher = bboxPattern.matcher(lineTitle);
                if (!bboxMatcher.find()) {
                    log.warn("ocr_line无bbox，跳过：{}", lineTitle);
                    continue;
                }

                // 解析坐标
                int x1 = Integer.parseInt(bboxMatcher.group(1));
                int y1 = Integer.parseInt(bboxMatcher.group(2));
                int x2 = Integer.parseInt(bboxMatcher.group(3));
                int y2 = Integer.parseInt(bboxMatcher.group(4));

                // 提取行文本（兼容 ocrx_word / ocr_word / ocr_text）
                Elements wordElems = lineElem.select("span.ocrx_word, span.ocr_word, span.ocr_text");
                String lineRawText;

                if (wordElems.isEmpty()) {
                    // 有些 HOCR 结构里，文字直接挂在 ocr_line 上，没有拆成 word span
                    lineRawText = lineElem.text();
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Element wordElem : wordElems) {
                        String w = wordElem.text();
                        if (w != null && !w.trim().isEmpty()) {
                            sb.append(w).append(" ");
                        }
                    }
                    lineRawText = sb.toString();
                }

                String cleanLineText = cleanOcrText(lineRawText == null ? "" : lineRawText.trim());
                if (cleanLineText.isEmpty()) {
                    log.warn("行文本清洗后为空，跳过");
                    continue;
                }

                // 计算排版特征（适配动态页面尺寸）
                OcrLine ocrLine = new OcrLine();
                ocrLine.setPageNum(pageNum);
                ocrLine.setText(cleanLineText);
                ocrLine.setX1(x1);
                ocrLine.setY1(y1);
                ocrLine.setX2(x2);
                ocrLine.setY2(y2);
                ocrLine.setWidth(x2 - x1);
                ocrLine.setHeight(y2 - y1);

                // 估算字号（行高≈字号*1.2，扫描件适配）
                ocrLine.setFontSize((int) (ocrLine.getHeight() / 1.2));

                // 判断是否居中（阈值放宽到150px，适配扫描件偏移）
                int lineCenterX = (x1 + x2) / 2;
                int pageCenterX = pageWidth / 2;
                ocrLine.setCentered(Math.abs(lineCenterX - pageCenterX) <= 150);

                // 判断是否在顶部（页面上1/3区域）
                ocrLine.setTopArea(y1 < pageHeight / 3);

                lines.add(ocrLine);
                log.info("解析到有效行：页码={}，文本={}，坐标=({},{})-({},{})",
                        pageNum, cleanLineText, x1, y1, x2, y2);
            }
        } catch (Exception e) {
            log.error("HOCR解析异常", e);
            // 最终降级：纯文本提取
            String plainText = Jsoup.parse(hocr).text();
            String cleanText = cleanOcrText(plainText);
            if (!cleanText.isEmpty()) {
                OcrLine fallbackLine = new OcrLine();
                fallbackLine.setPageNum(pageNum);
                fallbackLine.setText(cleanText);
                lines.add(fallbackLine);
            }
        }

        log.info("parse结束，共解析{}行有效文本", lines.size());
        return lines;
    }



    private String cleanOcrText(String ocrText) {
        if (ocrText == null) {
            log.info("空的");
            return "";
        }
        String raw = ocrText.trim();
        if (raw.isEmpty()) {
            log.info("空的");
            return "";
        }

        // 1. 合并多余空白
        String cleanText = raw.replaceAll("\\s+", " ");

        // 2. 只去掉特别奇怪的符号，保留中/英/数字/常用标点
        //    这里不过度过滤，后续匹配章节再考虑规范化
        cleanText = cleanText.replaceAll("[^\\u4e00-\\u9fa5A-Za-z0-9第章节\\.·()（）/、，,：:；;\\-\\s]", "");

        // 3. 再次 trim
        cleanText = cleanText.trim();

        log.info("清洗前：，清洗后：", ocrText, cleanText);
        return cleanText;
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        // 1. 确保Mat是8位3通道（BGR）
        if (mat.channels() == 1) {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGR);
        }
        // 2. 提取byte数组
        byte[] data = new byte[mat.channels() * mat.cols() * mat.rows()];
        mat.get(0, 0, data);
        // 3. 创建BufferedImage（TYPE_3BYTE_BGR）
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);
        System.arraycopy(data, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData(), 0, data.length);
        return image;
    }

    // OpenCV与BufferedImage互转工具方法（需引入OpenCV依赖）
    private Mat bufferedImageToMat(BufferedImage image) {
        // 1. 先将图像转换为TYPE_3BYTE_BGR（byte数组格式），兼容所有输入格式
        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = convertedImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // 2. 提取byte数组（此时DataBuffer必然是DataBufferByte）
        byte[] data = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
        // 3. 创建OpenCV Mat（BGR格式，对应Java的RGB）
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    /**
     * OCR提取图片PDF文本（修复路径问题）
     */
    private String extractTextByOCR(File pdfFile) throws Exception {
        ITesseract tesseract = new Tesseract();
        // ========== 核心修复：Windows路径配置 ==========
        // 方式1：使用绝对路径（推荐，避免环境变量问题）
        String tessDataPath = "G:\\Tesseract-OCR\\tessdata"; // 替换为你的实际路径
        // 方式2：如果安装在G盘，修正路径格式
        // String tessDataPath = "G:\\Tesseract-OCR\\tessdata";

        tesseract.setDatapath(tessDataPath); // 设置训练数据路径
        tesseract.setLanguage("chi_sim"); // 简体中文（必须确保chi_sim.traineddata存在）

        // 验证路径是否存在
        File tessDataDir = new File(tessDataPath);
        if (!tessDataDir.exists()) {
            throw new RuntimeException("Tesseract训练数据目录不存在：" + tessDataPath);
        }
        File chiSimFile = new File(tessDataPath + "\\chi_sim.traineddata");
        if (!chiSimFile.exists()) {
            throw new RuntimeException("简体中文训练数据缺失：" + chiSimFile.getAbsolutePath());
        }

        // PDF转图片（300dpi保证清晰度）
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                images.add(image);
            }
        }

        // OCR识别每张图片
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < images.size(); i++) {
            try {
                String pageText = tesseract.doOCR(images.get(i));
                log.info("[OCR-页{}] 识别文本长度：{}，内容预览：{}",
                        i+1, pageText.length(),
                        pageText.substring(0, Math.min(pageText.length(), 200)));
                sb.append(pageText).append("\n");
            } catch (Exception e) {
                log.error("[OCR-页{}] 识别失败", i+1, e);
            }
        }
        return sb.toString();
    }

    /**
     * 添加章节（打印详细解析结果）
     */
    private void addChapter(List<Chapter> chapters, Long textbookId, Long parentId, int level, String title, int pdfPage) {
        // 去重逻辑（优化：忽略空格/标点差异）
        String titleNormalized = title.replaceAll("\\s+", "").replaceAll("[，。、：；]", "");
        boolean duplicate = chapters.stream()
                .anyMatch(c -> c.getLevel() == level
                        && c.getTitle().replaceAll("\\s+", "").replaceAll("[，。、：；]", "").equals(titleNormalized)
                        && c.getPdfPage() == pdfPage);
        if (duplicate) {
            log.warn("章节重复，跳过：层级{} | 标题{} | 页码{}", level, title, pdfPage);
            return;
        }

        // 计算排序号
        int sort = (int) chapters.stream()
                .filter(c -> Objects.equals(c.getParentId(), parentId))
                .count() + 1;

        // 构建章节对象
        Chapter chapter = new Chapter();
        chapter.setTextbookId(textbookId);
        chapter.setParentId(parentId);
        chapter.setLevel(level);
        chapter.setTitle(title);
        chapter.setSort(sort);
        chapter.setPdfPage(pdfPage);
        chapter.setCreateTime(LocalDateTime.now());
        chapters.add(chapter);

        // 打印解析详情
        String levelDesc = switch (level) {
            case 1 -> "一级（章）";
            case 2 -> "二级（节）";
            case 3 -> "三级（小节）";
            default -> "未知层级";
        };
        log.info("[新增章节] {} | 标题：{} | 页码：{} | 父章节ID：{} | 排序号：{}",
                levelDesc, title, pdfPage, parentId == null ? "无" : parentId, sort);
    }
    /**
     * 从MinIO下载文件到本地临时目录
     */
    private File downloadMinIOFile(String bucket, String objectName) throws Exception {
        File tempFile = new File(System.getProperty("java.io.tmpdir") + File.separator + objectName);
        // 下载MinIO文件到临时文件
        minioClient.downloadObject(
                DownloadObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .filename(tempFile.getAbsolutePath())
                        .build()
        );
        return tempFile;
    }

    // 注入MinIO客户端（用于下载文件）
    @Autowired
    @Qualifier("innerMinioClient")
    private MinioClient innerMinioClient;
}
