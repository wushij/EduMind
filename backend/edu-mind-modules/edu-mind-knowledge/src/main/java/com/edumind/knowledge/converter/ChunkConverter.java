package com.edumind.knowledge.converter;

import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.edumind.knowledge.vo.knowledge.ChunkVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChunkConverter {

    public ChunkVO toVO(KnowledgeDocumentChunkEntity entity) {
        if (entity == null) {
            return null;
        }
        ChunkVO vo = new ChunkVO();
        vo.setId(entity.getId());
        vo.setDocumentId(entity.getDocumentId());
        vo.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        vo.setChunkIndex(entity.getChunkIndex());
        vo.setContent(entity.getContent());
        vo.setPageNo(entity.getPageNo());
        vo.setHeading(entity.getHeading());
        vo.setCharCount(entity.getCharCount());
        vo.setTokenEstimate(entity.getTokenEstimate());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    public List<ChunkVO> toVOList(List<KnowledgeDocumentChunkEntity> entities) {
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }
}
