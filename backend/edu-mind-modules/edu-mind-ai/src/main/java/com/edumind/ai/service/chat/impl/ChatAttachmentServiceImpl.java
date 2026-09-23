package com.edumind.ai.service.chat.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.service.chat.ChatAttachmentService;
import com.edumind.ai.vo.chat.ChatAttachmentVO;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.knowledge.api.DocumentParseApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAttachmentServiceImpl implements ChatAttachmentService {

    private final DocumentParseApi documentParseApi;
    private final RedisService redisService;
    private final FileStorageService fileStorageService;

    @Value("${edumind.oss.bucket-name:edumind}")
    private String bucketName;

    private static final String REDIS_KEY_PREFIX = "edumind:ai:attachment:";
    private static final long ATTACHMENT_TTL_SECONDS = 86400L; // 24小时有效
    private static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L; // 20MB

    @Override
    public ChatAttachmentVO uploadAndParse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传的附件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("文件大小超出限制，单个附件最大支持 20MB");
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "attachment.txt";
        String contentType = file.getContentType() != null ? file.getContentType() : "text/plain";
        long sizeBytes = file.getSize();
        String attachmentId = UUID.randomUUID().toString().replace("-", "");

        // 1. 尝试上传到对象存储作为长效留存备用
        String objectKey = "chat-attachments/" + attachmentId + "/" + originalFilename;
        try {
            fileStorageService.uploadFile(bucketName, objectKey, file.getInputStream(), contentType);
        } catch (Exception ex) {
            log.warn("附件上传至持久存储警告（继续内存解析）: {}", ex.getMessage());
        }

        // 2. 解析正文纯文本
        String extractedText = "";
        String parseStatus = "SUCCESS";
        String errorMessage = null;
        try {
            byte[] bytes = file.getBytes();
            extractedText = documentParseApi.extractText(bytes, contentType, originalFilename);
            if (extractedText == null) {
                extractedText = "";
            }
        } catch (Exception ex) {
            log.error("附件解析失败: filename={}, error={}", originalFilename, ex.getMessage(), ex);
            parseStatus = "FAILED";
            errorMessage = "文档内容解析失败: " + ex.getMessage();
        }

        // 3. 构建预览内容
        String previewText = extractedText.length() > 200 ? extractedText.substring(0, 200) + "..." : extractedText;

        // 4. 将解析出的内容缓存入 Redis，供聊天生成时极速读取
        JSONObject payload = new JSONObject();
        payload.put("attachmentId", attachmentId);
        payload.put("fileName", originalFilename);
        payload.put("fileType", contentType);
        payload.put("fileSizeBytes", sizeBytes);
        payload.put("text", extractedText);
        payload.put("objectKey", objectKey);
        redisService.set(REDIS_KEY_PREFIX + attachmentId, payload.toJSONString(), ATTACHMENT_TTL_SECONDS);

        return ChatAttachmentVO.builder()
                .attachmentId(attachmentId)
                .fileName(originalFilename)
                .fileSizeText(formatFileSize(sizeBytes))
                .fileSizeBytes(sizeBytes)
                .fileType(getFileExtension(originalFilename))
                .previewText(previewText)
                .charCount(extractedText.length())
                .parseStatus(parseStatus)
                .errorMessage(errorMessage)
                .build();
    }

    @Override
    public String getAttachmentText(String attachmentId) {
        if (!StringUtils.hasText(attachmentId)) {
            return "";
        }
        String json = redisService.get(REDIS_KEY_PREFIX + attachmentId);
        if (!StringUtils.hasText(json)) {
            return "";
        }
        try {
            JSONObject obj = JSON.parseObject(json);
            return obj.getString("text");
        } catch (Exception ex) {
            log.warn("读取附件内容缓存失败: {}", ex.getMessage());
            return "";
        }
    }

    @Override
    public String buildAttachmentsContext(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int validCount = 0;
        for (String id : attachmentIds) {
            if (!StringUtils.hasText(id)) continue;
            String json = redisService.get(REDIS_KEY_PREFIX + id);
            if (!StringUtils.hasText(json)) continue;
            try {
                JSONObject obj = JSON.parseObject(json);
                String fileName = obj.getString("fileName");
                String text = obj.getString("text");
                if (StringUtils.hasText(text)) {
                    validCount++;
                    sb.append("【用户附件").append(validCount).append("：").append(fileName).append("】\n");
                    // 限制单个附件最大字符数，避免 prompt 溢出
                    String truncatedText = text.length() > 6000 ? text.substring(0, 6000) + "\n(已截断超长部分...)" : text;
                    sb.append(truncatedText).append("\n\n");
                }
            } catch (Exception ex) {
                log.warn("解析附件上下文异常: id={}, error={}", id, ex.getMessage());
            }
        }
        if (validCount == 0) {
            return "";
        }
        return "\n--- 用户上传的附件参考正文 ---\n" + sb.toString().trim() + "\n-----------------------------\n";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) return "FILE";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            return filename.substring(dot + 1).toUpperCase();
        }
        return "FILE";
    }
}
