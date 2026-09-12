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
import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.service.chunk.ChunkSplitter;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.vo.knowledge.ChunkStatsVO;
import com.edumind.knowledge.vo.knowledge.ChunkTaskVO;
import com.edumind.knowledge.vo.knowledge.ChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkTaskVO triggerChunk(Long documentId) {
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        knowledgeAccessService.assertAccessible(document.getKnowledgeBaseId());
        if (!"SUCCESS".equals(document.getParseStatus())) {
            throw new BusinessException("请先完成文档解析");
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
    public PageResult<ChunkVO> pageChunks(Long documentId, long page, long pageSize, String keyword) {
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        knowledgeAccessService.assertAccessible(document.getKnowledgeBaseId());
        long currentPage = Math.max(page, 1);
        long size = Math.min(Math.max(pageSize, 1), 100);
        Page<KnowledgeDocumentChunkEntity> result = knowledgeDocumentChunkDao.pageByDocumentId(
                documentId, currentPage, size, keyword);
        return PageResult.<ChunkVO>builder()
                .total(result.getTotal())
                .pageNum(currentPage)
                .pageSize(size)
                .list(chunkConverter.toVOList(result.getRecords()))
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

    private void refreshChunkCount(Long knowledgeBaseId) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseDao.findById(knowledgeBaseId);
        if (knowledgeBase != null) {
            knowledgeBase.setChunkCount((int) knowledgeDocumentChunkDao.countByKnowledgeBaseId(knowledgeBaseId));
            knowledgeBaseDao.updateById(knowledgeBase);
        }
    }
}
