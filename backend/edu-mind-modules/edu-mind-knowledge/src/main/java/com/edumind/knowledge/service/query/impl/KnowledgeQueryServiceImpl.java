package com.edumind.knowledge.service.query.impl;

import com.edumind.knowledge.converter.KnowledgeBaseConverter;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeDocumentTextDao;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentTextEntity;
import com.edumind.knowledge.service.query.KnowledgeQueryService;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryServiceImpl implements KnowledgeQueryService {

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    private final KnowledgeDocumentTextDao knowledgeDocumentTextDao;
    private final KnowledgeBaseConverter knowledgeBaseConverter;

    @Override
    public KnowledgeBaseVO getKnowledgeBaseById(Long knowledgeBaseId) {
        return knowledgeBaseConverter.toVO(knowledgeBaseDao.findById(knowledgeBaseId));
    }

    @Override
    public List<KnowledgeBaseVO> listKnowledgeBasesByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return knowledgeBaseDao.findByCourseId(courseId).stream()
                .map(knowledgeBaseConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeDocumentVO> listDocumentsByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return Collections.emptyList();
        }
        return knowledgeDocumentDao.findByKnowledgeBaseId(knowledgeBaseId).stream()
                .map(knowledgeBaseConverter::toDocumentVO)
                .collect(Collectors.toList());
    }

    @Override
    public String getDocumentText(Long documentId) {
        KnowledgeDocumentTextEntity textEntity = knowledgeDocumentTextDao.findByDocumentId(documentId);
        return textEntity != null ? textEntity.getContent() : null;
    }

    @Override
    public KnowledgeDocumentVO getDocumentById(Long documentId) {
        if (documentId == null) {
            return null;
        }
        KnowledgeDocumentEntity entity = knowledgeDocumentDao.findById(documentId);
        KnowledgeDocumentVO vo = knowledgeBaseConverter.toDocumentVO(entity);
        if (vo != null) {
            vo.setChunkCount((int) knowledgeDocumentChunkDao.countByDocumentId(documentId));
        }
        return vo;
    }
}
