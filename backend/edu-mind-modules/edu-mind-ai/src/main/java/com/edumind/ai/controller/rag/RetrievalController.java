package com.edumind.ai.controller.rag;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.converter.RagConverter;
import com.edumind.ai.dto.rag.RetrievalRequestDTO;
import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.pipeline.RagPipelineImpl;
import com.edumind.ai.vo.rag.RetrievalResultVO;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.api.KnowledgeAccessApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class RetrievalController {

    private final RagPipelineImpl ragPipeline;
    private final RagConverter ragConverter;
    private final KnowledgeAccessApi knowledgeAccessApi;

    @SaCheckPermission("knowledge:view")
    @PostMapping("/{id}/retrieve")
    public ApiResult<List<RetrievalResultVO>> retrieve(@PathVariable("id") Long knowledgeBaseId,
                                                       @Valid @RequestBody RetrievalRequestDTO request) {
        knowledgeAccessApi.assertAccessible(knowledgeBaseId);
        RagResult result = ragPipeline.executeDetailed(
                request.getQuery(),
                knowledgeBaseId,
                request.getTopK() != null ? request.getTopK() : 5,
                request.getMinScore() != null ? request.getMinScore() : 0.65,
                request.getDocumentId(),
                true
        );
        return ApiResult.success(ragConverter.toVOList(result.getRetrievalResults()));
    }
}
