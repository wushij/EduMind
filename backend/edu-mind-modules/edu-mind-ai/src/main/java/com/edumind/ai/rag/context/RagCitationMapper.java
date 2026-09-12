package com.edumind.ai.rag.context;

import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.vo.rag.CitationVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class RagCitationMapper {

    private RagCitationMapper() {
    }

    public static List<CitationVO> toCitations(RagResult ragResult) {
        if (ragResult == null || ragResult.getRetrievalResults() == null) {
            return Collections.emptyList();
        }
        return ragResult.getRetrievalResults().stream()
                .map(RagCitationMapper::toCitation)
                .collect(Collectors.toList());
    }

    public static CitationVO toCitation(RetrievalHit hit) {
        CitationVO citation = new CitationVO();
        citation.setDocumentName(hit.getDocumentName());
        citation.setPageNo(hit.getPageNo());
        citation.setChunkId(hit.getChunkId());
        citation.setScore(hit.getScore());
        citation.setExcerpt(hit.getExcerpt());
        citation.setChunkIndex(hit.getChunkIndex());
        return citation;
    }
}
