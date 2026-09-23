package com.edumind.knowledge.service.knowledge.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.utils.TenantObjectKeyBuilder;
import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.knowledge.converter.KnowledgeBaseConverter;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeDocumentTextDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentTextEntity;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.DocumentParseService;
import com.edumind.knowledge.service.knowledge.DocumentService;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final KnowledgeDocumentTextDao knowledgeDocumentTextDao;
    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    private final KnowledgeBaseConverter knowledgeBaseConverter;
    private final DocumentParseService documentParseService;
    private final FileStorageService fileStorageService;
    private final IndexingService indexingService;

    @Value("${minio.bucketName:edumind}")
    private String bucketName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO upload(Long knowledgeBaseId, MultipartFile file) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseDao.findById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        Long tenantId = knowledgeBase.getTenantId() != null ? knowledgeBase.getTenantId() : TenantContext.getTenantId();
        String objectKey = TenantObjectKeyBuilder.knowledgeDocument(tenantId, knowledgeBaseId, UUID.randomUUID().toString(), originalFilename);
        try {
            fileStorageService.uploadFile(bucketName, objectKey, file.getInputStream(), file.getContentType());
        } catch (IOException ex) {
            throw new BusinessException("文件上传失败");
        }
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setFileName(originalFilename);
        entity.setFileType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setObjectKey(objectKey);
        entity.setParseStatus("PENDING");
        entity.setStatus(1);
        knowledgeDocumentDao.insert(entity);
        refreshDocCount(knowledgeBaseId);
        return knowledgeBaseConverter.toDocumentVO(entity);
    }

    @Override
    public KnowledgeDocumentVO getById(Long documentId) {
        KnowledgeDocumentEntity entity = knowledgeDocumentDao.findById(documentId);
        if (entity == null) {
            throw new BusinessException("文档不存在");
        }
        return toDocumentVO(entity);
    }

    @Override
    public List<KnowledgeDocumentVO> list(Long knowledgeBaseId) {
        return knowledgeDocumentDao.findByKnowledgeBaseId(knowledgeBaseId).stream()
                .map(this::toDocumentVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long documentId) {
        KnowledgeDocumentEntity entity = knowledgeDocumentDao.findById(documentId);
        if (entity == null) {
            throw new BusinessException("文档不存在");
        }
        // 先清向量与索引行：切片删掉后这些行会变成孤儿，让「已向量化」统计与检索召回失真
        indexingService.purgeDocumentVectors(documentId);
        if (ownsStoredObject(entity)) {
            deleteStoredObject(entity.getObjectKey());
        }
        knowledgeDocumentChunkDao.deleteByDocumentId(documentId);
        knowledgeDocumentTextDao.deleteByDocumentId(documentId);
        knowledgeDocumentDao.deleteById(documentId);
        refreshDocCount(entity.getKnowledgeBaseId());
    }

    /**
     * 只有「上传文档」的文件才归知识库自己所有。
     * 课件同步（COURSE_RESOURCE）与课节讲义（LESSON）的 objectKey 指向课程资料/讲义本体，
     * 删除知识库文档时不能连带删掉课程侧仍在使用的源文件。
     */
    private boolean ownsStoredObject(KnowledgeDocumentEntity entity) {
        if (!StringUtils.hasText(entity.getObjectKey())) {
            return false;
        }
        String sourceType = entity.getSourceType();
        return !StringUtils.hasText(sourceType) || "UPLOAD".equalsIgnoreCase(sourceType);
    }

    private void deleteStoredObject(String objectKey) {
        try {
            fileStorageService.deleteFile(bucketName, objectKey);
        } catch (Exception ex) {
            // 对象存储不可用时也必须允许清理知识库记录，否则用户会卡在"删不掉"
            log.warn("删除文档存储对象失败 objectKey={}: {}", objectKey, ex.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerParse(Long documentId) {
        KnowledgeDocumentEntity entity = knowledgeDocumentDao.findById(documentId);
        if (entity == null) {
            throw new BusinessException("文档不存在");
        }
        if ("LESSON".equalsIgnoreCase(entity.getSourceType())) {
            reconcileLessonDocument(entity);
            if ("SUCCESS".equals(entity.getParseStatus()) || "CHUNKED".equals(entity.getParseStatus())) {
                return;
            }
            throw new BusinessException("课节讲义无正文，请在课程中编辑并发布课节后再索引");
        }
        entity.setParseStatus("PARSING");
        entity.setErrorMessage(null);
        knowledgeDocumentDao.updateById(entity);
        try {
            var inputStream = fileStorageService.getFile(bucketName, entity.getObjectKey());
            if (inputStream == null) {
                throw new BusinessException("文档文件不存在或无法读取");
            }
            byte[] bytes = inputStream.readAllBytes();
            String text = documentParseService.extractText(bytes, entity.getFileType(), entity.getFileName());
            KnowledgeDocumentTextEntity textEntity = knowledgeDocumentTextDao.findByDocumentId(documentId);
            if (textEntity == null) {
                textEntity = new KnowledgeDocumentTextEntity();
                textEntity.setDocumentId(documentId);
                textEntity.setContent(text);
                knowledgeDocumentTextDao.insert(textEntity);
            } else {
                textEntity.setContent(text);
                knowledgeDocumentTextDao.updateById(textEntity);
            }
            entity.setParseStatus("SUCCESS");
            entity.setErrorMessage(null);
        } catch (Exception ex) {
            entity.setParseStatus("FAILED");
            entity.setErrorMessage(ex.getMessage());
        }
        knowledgeDocumentDao.updateById(entity);
    }

    private KnowledgeDocumentVO toDocumentVO(KnowledgeDocumentEntity entity) {
        reconcileLessonDocument(entity);
        KnowledgeDocumentVO vo = knowledgeBaseConverter.toDocumentVO(entity);
        if (vo != null && entity.getId() != null) {
            long count = knowledgeDocumentChunkDao.countByDocumentId(entity.getId());
            vo.setChunkCount((int) count);
            vo.setChunkStatus(resolveChunkStatus(entity.getParseStatus(), count));
            vo.setParseStatus(entity.getParseStatus());
        }
        return vo;
    }

    /**
     * 课节虚拟文档不走 MinIO 解析；误点「解析」会标 FAILED，此处按正文+切片纠正为 SUCCESS。
     */
    private void reconcileLessonDocument(KnowledgeDocumentEntity entity) {
        if (entity == null || !"LESSON".equalsIgnoreCase(entity.getSourceType())) {
            return;
        }
        long count = knowledgeDocumentChunkDao.countByDocumentId(entity.getId());
        KnowledgeDocumentTextEntity text = knowledgeDocumentTextDao.findByDocumentId(entity.getId());
        boolean hasText = text != null && StringUtils.hasText(text.getContent());
        if (count > 0 && hasText) {
            String ps = entity.getParseStatus();
            if ("FAILED".equals(ps) || "PENDING".equals(ps) || "PARSING".equals(ps)) {
                entity.setParseStatus("SUCCESS");
                entity.setErrorMessage(null);
                knowledgeDocumentDao.updateById(entity);
            }
        }
    }

    private String resolveChunkStatus(String parseStatus, long chunkCount) {
        if ("CHUNKING".equals(parseStatus)) {
            return "CHUNKING";
        }
        if ("CHUNK_FAILED".equals(parseStatus)) {
            return "CHUNK_FAILED";
        }
        if ("CHUNKED".equals(parseStatus) || chunkCount > 0) {
            return "CHUNKED";
        }
        if ("SUCCESS".equals(parseStatus)) {
            return "PARSED";
        }
        return parseStatus;
    }

    private void refreshDocCount(Long knowledgeBaseId) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseDao.findById(knowledgeBaseId);
        if (knowledgeBase != null) {
            knowledgeBase.setDocCount((int) knowledgeDocumentDao.countByKnowledgeBaseId(knowledgeBaseId));
            knowledgeBaseDao.updateById(knowledgeBase);
        }
    }
}
