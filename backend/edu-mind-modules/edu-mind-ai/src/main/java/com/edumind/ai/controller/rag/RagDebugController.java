package com.edumind.ai.controller.rag;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.converter.RagConverter;
import com.edumind.ai.dto.rag.RagDebugRequestDTO;
import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.pipeline.RagPipelineImpl;
import com.edumind.ai.vo.rag.RagDebugResponseVO;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RagDebugController {

    private final RagPipelineImpl ragPipeline;
    private final RagConverter ragConverter;
    private final KnowledgeAccessService knowledgeAccessService;

    @SaCheckPermission("knowledge:rag:debug")
    @PostMapping("/debug")
    public ApiResult<RagDebugResponseVO> debug(@Valid @RequestBody RagDebugRequestDTO request) {
        knowledgeAccessService.assertAccessible(request.getKnowledgeBaseId());
        RagResult result = ragPipeline.executeDetailed(
                request.getQuery(),
                request.getKnowledgeBaseId(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getMinScore() != null ? request.getMinScore() : 0.65,
                request.getDocumentId(),
                Boolean.TRUE.equals(request.getSkipLlm())
        );
        RagDebugResponseVO vo = new RagDebugResponseVO();
        vo.setOriginalQuery(result.getOriginalQuery());
        vo.setRewrittenQuery(result.getRewrittenQuery());
        vo.setRetrievalResults(ragConverter.toVOList(result.getRetrievalResults()));
        vo.setContext(result.getContext());
        vo.setPromptPreview(result.getPromptPreview());
        vo.setAnswer(result.getAnswer());
        return ApiResult.success(vo);
    }
}
