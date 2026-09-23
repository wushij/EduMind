package com.edumind.knowledge.service.knowledge.impl;

import com.edumind.knowledge.api.DocumentParseApi;
import com.edumind.knowledge.service.knowledge.DocumentParseService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentParseServiceImpl implements DocumentParseService, DocumentParseApi {

    @Override
    public String extractText(byte[] fileBytes, String fileType, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            return "";
        }
        String normalizedType = fileType != null ? fileType.toLowerCase() : "";
        String normalizedName = fileName != null ? fileName.toLowerCase() : "";
        if (isMarkdown(normalizedType, normalizedName) || isPlainText(normalizedType, normalizedName)) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        }
        if (normalizedType.contains("pdf") || normalizedName.endsWith(".pdf")) {
            return extractPdfText(fileBytes);
        }
        if (normalizedType.contains("word")
                || normalizedType.contains("docx")
                || normalizedName.endsWith(".doc")
                || normalizedName.endsWith(".docx")) {
            return extractDocxText(fileBytes);
        }
        return new String(fileBytes, StandardCharsets.UTF_8);
    }

    private String extractPdfText(byte[] fileBytes) {
        try (PDDocument document = PDDocument.load(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception ex) {
            throw new IllegalStateException("PDF 解析失败: " + ex.getMessage(), ex);
        }
    }

    private String extractDocxText(byte[] fileBytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception ex) {
            throw new IllegalStateException("Word 文档解析失败: " + ex.getMessage(), ex);
        }
    }

    private boolean isMarkdown(String fileType, String fileName) {
        return fileType.contains("markdown") || fileName.endsWith(".md") || fileName.endsWith(".markdown");
    }

    private boolean isPlainText(String fileType, String fileName) {
        return fileType.contains("text") || fileName.endsWith(".txt");
    }
}
