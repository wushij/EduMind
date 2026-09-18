package com.edumind.knowledge.converter;

import com.edumind.knowledge.dto.knowledge.KnowledgeBaseCreateDTO;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseUpdateDTO;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseConverter {

    public KnowledgeBaseEntity toEntity(KnowledgeBaseCreateDTO dto) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCourseId(dto.getCourseId());
        entity.setDocCount(0);
        entity.setStatus(1);
        return entity;
    }

    public void applyUpdate(KnowledgeBaseEntity entity, KnowledgeBaseUpdateDTO dto) {
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getCourseId() != null) {
            entity.setCourseId(dto.getCourseId());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    public KnowledgeBaseVO toVO(KnowledgeBaseEntity entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setCourseId(entity.getCourseId());
        vo.setDocCount(entity.getDocCount());
        vo.setChunkCount(entity.getChunkCount());
        vo.setIndexStatus(entity.getIndexStatus());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public KnowledgeDocumentVO toDocumentVO(KnowledgeDocumentEntity entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        vo.setId(entity.getId());
        vo.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        vo.setSourceType(entity.getSourceType());
        vo.setFileName(entity.getFileName());
        vo.setFileType(entity.getFileType());
        vo.setFileSize(entity.getFileSize());
        vo.setObjectKey(entity.getObjectKey());
        vo.setParseStatus(entity.getParseStatus());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
