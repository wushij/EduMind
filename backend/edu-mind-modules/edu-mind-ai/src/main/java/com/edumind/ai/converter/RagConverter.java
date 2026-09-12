package com.edumind.ai.converter;

import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.vo.rag.RetrievalResultVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagConverter {

    public RetrievalResultVO toVO(RetrievalHit hit) {
        RetrievalResultVO vo = new RetrievalResultVO();
        vo.setChunkId(hit.getChunkId());
        vo.setScore(hit.getScore());
        vo.setDocumentId(hit.getDocumentId());
        vo.setDocumentName(hit.getDocumentName());
        vo.setPageNo(hit.getPageNo());
        vo.setExcerpt(hit.getExcerpt());
        vo.setMetadata(hit.getMetadata());
        return vo;
    }

    public List<RetrievalResultVO> toVOList(List<RetrievalHit> hits) {
        return hits.stream().map(this::toVO).collect(Collectors.toList());
    }
}
