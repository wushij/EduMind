package com.edumind.ai.controller.rag;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.converter.RagConverter;
import com.edumind.ai.dto.rag.RagDebugRequestDTO;
import com.edumind.ai.gateway.AiUserModelPolicy;
import com.edumind.ai.rag.model.RagDebugOptions;
import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.pipeline.RagPipelineImpl;
import com.edumind.ai.rag.query.QueryRewriteContext;
import com.edumind.ai.service.gateway.GatewayManageService;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import com.edumind.ai.vo.rag.RagDebugResponseVO;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.api.KnowledgeAccessApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RagDebugController {

    private final RagPipelineImpl ragPipeline;
    private final RagConverter ragConverter;
    private final KnowledgeAccessApi knowledgeAccessApi;
    private final GatewayManageService gatewayManageService;
    private final AiUserModelPolicy aiUserModelPolicy;

    /**
     * 诊断工作台可选模型列表。
     *
     * <p>复用网关的「已启用对话模型」口径：排除 mock、只返回平台允许用户自选的模型，
     * 避免诊断页出现用户账号里根本不存在的模型。权限跟随诊断页（knowledge:rag:debug），
     * 而不是 ai:chat，保证能进诊断页的角色一定能拿到模型列表。</p>
     */
    @SaCheckPermission("knowledge:rag:debug")
    @GetMapping("/models")
    public ApiResult<List<AiModelConfigVO>> listDebugModels() {
        return ApiResult.success(gatewayManageService.listEnabledChatModels());
    }

    @SaCheckPermission("knowledge:rag:debug")
    @PostMapping("/debug")
    public ApiResult<RagDebugResponseVO> debug(@Valid @RequestBody RagDebugRequestDTO request) {
        knowledgeAccessApi.assertAccessible(request.getKnowledgeBaseId());

        RagDebugOptions options = new RagDebugOptions(
                // 用户自选模型必须过治理策略（自选开关 + 白名单），未通过时回落平台策略
                aiUserModelPolicy.validateUserSelection(request.getModelKey()),
                request.getTemperature(),
                request.getSystemPrompt());

        RagResult result = ragPipeline.executeDetailed(
                request.getQuery(),
                request.getKnowledgeBaseId(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getMinScore() != null ? request.getMinScore() : 0.03,
                request.getDocumentId(),
                Boolean.TRUE.equals(request.getSkipLlm()),
                QueryRewriteContext.ofQuestion(request.getQuery()),
                options
        );

        RagDebugResponseVO vo = new RagDebugResponseVO();
        vo.setOriginalQuery(result.getOriginalQuery());
        vo.setRewrittenQuery(result.getRewrittenQuery());
        vo.setRetrievalResults(ragConverter.toVOList(result.getRetrievalResults()));
        vo.setContext(result.getContext());
        vo.setPromptPreview(result.getPromptPreview());
        vo.setAnswer(result.getAnswer());
        vo.setStageTimings(ragConverter.toStageTimingVOList(result.getStageTimings()));
        vo.setModelKey(result.getModelKey());
        vo.setSystemPromptOverridden(result.getSystemPromptOverridden());
        vo.setPromptTokens(result.getPromptTokens());
        vo.setCompletionTokens(result.getCompletionTokens());
        vo.setTotalTokens(result.getTotalTokens());
        vo.setTotalLatencyMs(result.getTotalLatencyMs());
        return ApiResult.success(vo);
    }
}
