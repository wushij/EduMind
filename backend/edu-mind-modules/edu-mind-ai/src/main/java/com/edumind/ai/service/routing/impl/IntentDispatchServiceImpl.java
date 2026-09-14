package com.edumind.ai.service.routing.impl;

import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.rag.pipeline.RagPipelineImpl;
import com.edumind.ai.rag.query.QueryRewriteContext;
import com.edumind.ai.router.IntentRouter;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.service.routing.IntentDispatchPlan;
import com.edumind.ai.service.routing.IntentDispatchRequest;
import com.edumind.ai.service.routing.IntentDispatchService;
import com.edumind.ai.vo.rag.CitationVO;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntentDispatchServiceImpl implements IntentDispatchService {

    private final IntentRouter intentRouter;
    private final PromptService promptService;
    private final RagPipelineImpl ragPipeline;
    private final KnowledgeAccessService knowledgeAccessService;
    private final CourseQueryApi courseQueryApi;

    @Override
    public IntentRouter.IntentResult route(String message, Long courseId) {
        return intentRouter.route(message, courseId);
    }

    @Override
    public IntentDispatchPlan prepare(IntentDispatchRequest request) {
        IntentRouter.IntentResult intent = request.getIntent() != null
                ? request.getIntent()
                : route(request.getMessage(), request.getCourseId());
        String routeType = intent.type() != null ? intent.type().toLowerCase() : "chat";

        return switch (routeType) {
            case "rag" -> buildRagPlan(request, intent);
            case "agent" -> buildAgentPlan(request, intent);
            case "navigate" -> buildNavigatePlan(request, intent);
            default -> buildChatPlan(request, intent);
        };
    }

    private IntentDispatchPlan buildChatPlan(IntentDispatchRequest request, IntentRouter.IntentResult intent) {
        return IntentDispatchPlan.builder()
                .route("chat")
                .agentCode(intent.targetCode())
                .targetCode(intent.targetCode())
                .systemPrompt(promptService.getSystemPrompt("chat"))
                .userPrompt(request.getMessage())
                .useRag(false)
                .build();
    }

    private IntentDispatchPlan buildRagPlan(IntentDispatchRequest request, IntentRouter.IntentResult intent) {
        String systemPrompt = promptService.getSystemPrompt("chat");
        String userPrompt = request.getMessage();
        List<CitationVO> citations = List.of();
        boolean useRag = request.getKnowledgeBaseId() != null;

        if (useRag) {
            knowledgeAccessService.assertAccessible(request.getKnowledgeBaseId());
            QueryRewriteContext rewriteContext = QueryRewriteContext.builder()
                    .question(request.getMessage())
                    .courseName(resolveCourseName(request))
                    .conversationHistory(request.getConversationHistory())
                    .build();
            RagResult ragResult = ragPipeline.executeDetailed(
                    request.getMessage(),
                    request.getKnowledgeBaseId(),
                    5,
                    0.65,
                    request.getDocumentId(),
                    false,
                    rewriteContext
            );
            userPrompt = ragResult.getPromptPreview();
            citations = toCitations(ragResult);
            Map<String, String> vars = new HashMap<>();
            vars.put("context", ragResult.getContext() != null ? ragResult.getContext() : "");
            vars.put("question", request.getMessage());
            systemPrompt = promptService.renderTemplate("chat_rag", vars);
        }

        return IntentDispatchPlan.builder()
                .route("rag")
                .agentCode(intent.targetCode())
                .targetCode(intent.targetCode())
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .useRag(useRag)
                .citations(citations)
                .build();
    }

    private IntentDispatchPlan buildAgentPlan(IntentDispatchRequest request, IntentRouter.IntentResult intent) {
        String agentCode = mapAgentCode(intent.targetCode());
        Map<String, String> vars = new HashMap<>();
        vars.put("intent", "agent:" + agentCode);
        vars.put("extraContext", "请结合教学场景给出可执行建议，必要时说明可触发的 Agent 能力。");
        return IntentDispatchPlan.builder()
                .route("agent")
                .agentCode(agentCode)
                .targetCode(intent.targetCode())
                .systemPrompt(promptService.renderTemplate("global_assistant", vars))
                .userPrompt(request.getMessage())
                .useRag(false)
                .build();
    }

    private IntentDispatchPlan buildNavigatePlan(IntentDispatchRequest request, IntentRouter.IntentResult intent) {
        String target = StringUtils.hasText(intent.targetCode()) ? intent.targetCode() : "/dashboard";
        Map<String, String> vars = new HashMap<>();
        vars.put("targetCode", target);
        vars.put("question", request.getMessage());
        Map<String, Object> navigate = new HashMap<>();
        navigate.put("path", target);
        navigate.put("confidence", intent.confidence());
        return IntentDispatchPlan.builder()
                .route("navigate")
                .targetCode(target)
                .systemPrompt(promptService.renderTemplate("navigate", vars))
                .userPrompt(request.getMessage())
                .useRag(false)
                .navigatePayload(navigate)
                .build();
    }

    private String mapAgentCode(String targetCode) {
        if (!StringUtils.hasText(targetCode)) {
            return "teaching";
        }
        return switch (targetCode) {
            case "exam", "paper_compose" -> "question";
            case "grading" -> "grading";
            case "learning", "kb_retrieval" -> "learning";
            default -> targetCode;
        };
    }

    private List<CitationVO> toCitations(RagResult ragResult) {
        if (ragResult == null || ragResult.getRetrievalResults() == null) {
            return List.of();
        }
        return ragResult.getRetrievalResults().stream().map(this::toCitation).collect(Collectors.toList());
    }

    private CitationVO toCitation(RetrievalHit hit) {
        CitationVO citation = new CitationVO();
        citation.setDocumentName(hit.getDocumentName());
        citation.setPageNo(hit.getPageNo());
        citation.setChunkId(hit.getChunkId());
        citation.setScore(hit.getScore());
        citation.setExcerpt(hit.getExcerpt());
        citation.setChunkIndex(hit.getChunkIndex());
        return citation;
    }

    private String resolveCourseName(IntentDispatchRequest request) {
        if (StringUtils.hasText(request.getCourseName())) {
            return request.getCourseName();
        }
        if (request.getCourseId() == null) {
            return "";
        }
        try {
            CourseDetailVO course = courseQueryApi.getCourseById(request.getCourseId());
            return course != null && StringUtils.hasText(course.getName()) ? course.getName() : "";
        } catch (Exception ex) {
            return "";
        }
    }
}
