package com.edumind.knowledge.service.knowledge.impl;

import com.edumind.common.exception.BusinessException;
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
        // 兜底按文本读取只对文本类格式有意义。PPT / Excel / 图片等二进制格式走到这里，
        // 会被当作 UTF-8 解析成乱码并静默入库——用户上传时不报错，只有检索时才发现资料是废的。
        // 因此这里显式拒绝，把问题暴露在上传阶段。
        String displayName = fileName != null && !fileName.isBlank()
                ? fileName
                : (fileType != null && !fileType.isBlank() ? fileType : "未知文件");
        throw new BusinessException("暂不支持该文件格式：" + displayName + "，请上传 PDF、Word、Markdown 或 TXT 文本");
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
