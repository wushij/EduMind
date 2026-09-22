package com.edumind.ai.converter;

import com.edumind.ai.rag.model.RagStageTiming;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.vo.rag.RagStageTimingVO;
import com.edumind.ai.vo.rag.RetrievalResultVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
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

    public RagStageTimingVO toStageTimingVO(RagStageTiming timing) {
        RagStageTimingVO vo = new RagStageTimingVO();
        vo.setStage(timing.getStage());
        vo.setStageName(timing.getStageName());
        vo.setDurationMs(timing.getDurationMs());
        vo.setStatus(timing.getStatus());
        vo.setSummary(timing.getSummary());
        return vo;
    }

    public List<RagStageTimingVO> toStageTimingVOList(List<RagStageTiming> timings) {
        if (timings == null || timings.isEmpty()) {
            return Collections.emptyList();
        }
        return timings.stream().map(this::toStageTimingVO).collect(Collectors.toList());
    }
}
