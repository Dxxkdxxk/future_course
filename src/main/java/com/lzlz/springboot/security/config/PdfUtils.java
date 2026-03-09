package com.lzlz.springboot.security.config;

// PdfUtils.java
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfUtils {

    /**
     * 从PDF文件中提取所有页面的文本内容
     * @param pdfFile PDF文件
     * @return 页面文本内容列表
     */
    public List<String> extractTextFromPdf(File pdfFile) {
        List<String> pages = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();

            int totalPages = document.getNumberOfPages();
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);
                pages.add(pageText);
            }

        } catch (IOException e) {
            throw new RuntimeException("PDF文件读取失败: " + e.getMessage(), e);
        }

        return pages;
    }

    /**
     * 从PDF文件路径提取文本内容
     * @param pdfPath PDF文件路径
     * @return 页面文本内容列表
     */
    public List<String> extractTextFromPdf(String pdfPath) {
        return extractTextFromPdf(new File(pdfPath));
    }

    /**
     * 获取PDF页面的详细信息（包含页面大小等）
     */
    public List<PdfPageInfo> getPdfPageInfo(File pdfFile) {
        List<PdfPageInfo> pageInfos = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();

            int totalPages = document.getNumberOfPages();
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);

                PdfPageInfo pageInfo = new PdfPageInfo();
                pageInfo.setPageNumber(pageNum);
                pageInfo.setContent(pageText);
                pageInfo.setLineCount(pageText.split("\n").length);

                pageInfos.add(pageInfo);
            }

        } catch (IOException e) {
            throw new RuntimeException("PDF文件读取失败: " + e.getMessage(), e);
        }

        return pageInfos;
    }

    /**
     * PDF页面信息类
     */
    public static class PdfPageInfo {
        private int pageNumber;
        private String content;
        private int lineCount;

        // getter和setter
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public int getLineCount() { return lineCount; }
        public void setLineCount(int lineCount) { this.lineCount = lineCount; }
    }
}
