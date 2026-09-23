package com.edumind.knowledge.service.chunk.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.knowledge.converter.ChunkConverter;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeChunkIndexDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeDocumentTextDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentTextEntity;
import com.edumind.knowledge.entity.KnowledgeChunkIndexEntity;
import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.service.chunk.ChunkSplitter;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import org.springframework.util.StringUtils;
import com.edumind.knowledge.vo.knowledge.ChunkStatsVO;
import com.edumind.knowledge.vo.knowledge.ChunkTaskVO;
import com.edumind.knowledge.vo.knowledge.ChunkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkServiceImpl implements ChunkService {

    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final KnowledgeDocumentTextDao knowledgeDocumentTextDao;
    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    private final KnowledgeBaseDao knowledgeBaseDao;
    private final ChunkSplitter chunkSplitter;
    private final ChunkConverter chunkConverter;
    private final KnowledgeAccessService knowledgeAccessService;
    private final KnowledgeChunkIndexDao knowledgeChunkIndexDao;
    private final IndexingService indexingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkTaskVO triggerChunk(Long documentId) {
        return triggerChunk(documentId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkTaskVO triggerChunk(Long documentId, boolean reindex) {
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        knowledgeAccessService.assertAccessible(document.getKnowledgeBaseId());
        if (!isReadyForRechunk(document)) {
            throw new BusinessException("请先完成文档解析（上传文档需解析成功；课节讲义请使用「发布」或 RAG 大盘同步）");
        }
        KnowledgeDocumentTextEntity textEntity = knowledgeDocumentTextDao.findByDocumentId(documentId);
        if (textEntity == null || textEntity.getContent() == null || textEntity.getContent().isBlank()) {
            throw new BusinessException("文档正文为空，无法切片");
        }

        String taskId = "chunk-" + UUID.randomUUID();
        document.setParseStatus("CHUNKING");
        document.setErrorMessage(null);
        knowledgeDocumentDao.updateById(document);

        try {
            knowledgeDocumentChunkDao.deleteByDocumentId(documentId);
            List<ChunkSplitter.SplitChunk> splits = chunkSplitter.split(textEntity.getContent());
            for (ChunkSplitter.SplitChunk split : splits) {
                KnowledgeDocumentChunkEntity chunk = new KnowledgeDocumentChunkEntity();
                chunk.setDocumentId(documentId);
                chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
                chunk.setChunkIndex(split.getChunkIndex());
                chunk.setContent(split.getContent());
                chunk.setPageNo(split.getPageNo());
                chunk.setHeading(split.getHeading());
                chunk.setCharCount(split.getCharCount());
                chunk.setTokenEstimate(split.getTokenEstimate());
                knowledgeDocumentChunkDao.insert(chunk);
            }
            document.setParseStatus("CHUNKED");
            document.setErrorMessage(null);
            knowledgeDocumentDao.updateById(document);
            refreshChunkCount(document.getKnowledgeBaseId());
            if (reindex) {
                try {
                    indexingService.reindexDocument(documentId);
                } catch (Exception ex) {
                    log.warn("Reindex after chunk failed documentId={}: {}", documentId, ex.getMessage());
                }
            }

            ChunkTaskVO vo = new ChunkTaskVO();
            vo.setTaskId(taskId);
            vo.setStatus("CHUNKED");
            vo.setChunkCount(splits.size());
            return vo;
        } catch (Exception ex) {
            document.setParseStatus("CHUNK_FAILED");
            document.setErrorMessage(ex.getMessage());
            knowledgeDocumentDao.updateById(document);
            throw new BusinessException("文档切片失败: " + ex.getMessage());
        }
    }

    @Override
    public PageResult<ChunkVO> pageChunks(Long documentId, long page, long pageSize, String keyword,
                                          String embedStatus) {
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        knowledgeAccessService.assertAccessible(document.getKnowledgeBaseId());
        long currentPage = Math.max(page, 1);
        long size = Math.min(Math.max(pageSize, 1), 500);
        String statusFilter = normalizeEmbedStatusFilter(embedStatus);
        if (statusFilter != null) {
            List<KnowledgeDocumentChunkEntity> all = knowledgeDocumentChunkDao.findByDocumentId(documentId);
            List<ChunkVO> filtered = all.stream()
                    .map(entity -> enrichStatus(chunkConverter.toVO(entity)))
                    .filter(vo -> matchesKeyword(vo, keyword))
                    .filter(vo -> statusFilter.equals(vo.getStatus()))
                    .toList();
            long total = filtered.size();
            int from = (int) Math.min((currentPage - 1) * size, total);
            int to = (int) Math.min(from + size, total);
            List<ChunkVO> pageList = from < to ? filtered.subList(from, to) : List.of();
            return PageResult.<ChunkVO>builder()
                    .total(total)
                    .pageNum(currentPage)
                    .pageSize(size)
                    .list(pageList)
                    .build();
        }
        Page<KnowledgeDocumentChunkEntity> result = knowledgeDocumentChunkDao.pageByDocumentId(
                documentId, currentPage, size, keyword);
        List<ChunkVO> list = result.getRecords().stream()
                .map(entity -> enrichStatus(chunkConverter.toVO(entity)))
                .toList();
        return PageResult.<ChunkVO>builder()
                .total(result.getTotal())
                .pageNum(currentPage)
                .pageSize(size)
                .list(list)
                .build();
    }

    @Override
    public ChunkStatsVO getChunkStats(Long knowledgeBaseId) {
        knowledgeAccessService.assertAccessible(knowledgeBaseId);
        long total = knowledgeDocumentChunkDao.countByKnowledgeBaseId(knowledgeBaseId);
        long indexed = knowledgeChunkIndexDao.countIndexedByKnowledgeBaseId(knowledgeBaseId);
        long failed = knowledgeChunkIndexDao.findFailedByKnowledgeBaseId(knowledgeBaseId).size();
        long pending = Math.max(0, total - indexed - failed);

        List<KnowledgeDocumentChunkEntity> chunks = knowledgeDocumentChunkDao.findByKnowledgeBaseId(knowledgeBaseId);
        long tokenSum = chunks.stream()
                .mapToLong(chunk -> chunk.getTokenEstimate() != null ? chunk.getTokenEstimate() : 0)
                .sum();
        int avgTokens = chunks.isEmpty() ? 0 : (int) (tokenSum / chunks.size());

        ChunkStatsVO vo = new ChunkStatsVO();
        vo.setTotalChunks(total);
        vo.setIndexedChunks(indexed);
        vo.setPendingChunks(pending);
        vo.setFailedChunks(failed);
        vo.setAvgTokens(avgTokens);
        vo.setTotalTokens(tokenSum);
        return vo;
    }

    private boolean isReadyForRechunk(KnowledgeDocumentEntity document) {
        if ("LESSON".equalsIgnoreCase(document.getSourceType())) {
            return true;
        }
        String ps = document.getParseStatus();
        return "SUCCESS".equals(ps) || "CHUNKED".equals(ps);
    }

    private ChunkVO enrichStatus(ChunkVO vo) {
        if (vo == null || vo.getId() == null) {
            return vo;
        }
        KnowledgeChunkIndexEntity index = knowledgeChunkIndexDao.findByChunkId(vo.getId());
        if (index == null || !StringUtils.hasText(index.getEmbedStatus())) {
            vo.setStatus("PENDING");
        } else if ("FAILED".equalsIgnoreCase(index.getEmbedStatus())) {
            vo.setStatus("INDEX_FAILED");
        } else if ("INDEXED".equalsIgnoreCase(index.getEmbedStatus())) {
            vo.setStatus("INDEXED");
        } else {
            vo.setStatus("PENDING");
        }
        return vo;
    }

    private String normalizeEmbedStatusFilter(String embedStatus) {
        if (!StringUtils.hasText(embedStatus) || "ALL".equalsIgnoreCase(embedStatus)) {
            return null;
        }
        if ("INDEX_FAILED".equalsIgnoreCase(embedStatus)) {
            return "INDEX_FAILED";
        }
        if ("INDEXED".equalsIgnoreCase(embedStatus) || "PENDING".equalsIgnoreCase(embedStatus)) {
            return embedStatus.toUpperCase();
        }
        return null;
    }

    private boolean matchesKeyword(ChunkVO vo, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String k = keyword.trim().toLowerCase();
        return (vo.getContent() != null && vo.getContent().toLowerCase().contains(k))
                || (vo.getHeading() != null && vo.getHeading().toLowerCase().contains(k));
    }

    private void refreshChunkCount(Long knowledgeBaseId) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseDao.findById(knowledgeBaseId);
        if (knowledgeBase != null) {
            knowledgeBase.setChunkCount((int) knowledgeDocumentChunkDao.countByKnowledgeBaseId(knowledgeBaseId));
            knowledgeBaseDao.updateById(knowledgeBase);
        }
    }
}
