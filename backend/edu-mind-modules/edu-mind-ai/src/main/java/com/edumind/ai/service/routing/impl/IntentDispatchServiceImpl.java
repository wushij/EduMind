package com.edumind.ai.service.routing.impl;

import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.rag.config.RagProperties;
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
import com.edumind.knowledge.api.KnowledgeAccessApi;
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
    private final KnowledgeAccessApi knowledgeAccessApi;
    private final CourseQueryApi courseQueryApi;
    private final RagProperties ragProperties;

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
        String systemPrompt = promptService.getSystemPrompt("chat");
        String userPrompt = request.getMessage();
        List<CitationVO> citations = List.of();
        boolean useRag = request.getKnowledgeBaseId() != null;

        if (useRag) {
            RagAugmentation augmentation = augmentWithKnowledgeBase(request);
            systemPrompt = augmentation.systemPrompt();
            citations = augmentation.citations();
        }

        return IntentDispatchPlan.builder()
                .route("chat")
                .agentCode(intent.targetCode())
                .targetCode(intent.targetCode())
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .useRag(useRag)
                .citations(citations)
                .build();
    }

    private IntentDispatchPlan buildRagPlan(IntentDispatchRequest request, IntentRouter.IntentResult intent) {
        String systemPrompt = promptService.getSystemPrompt("chat");
        String userPrompt = request.getMessage();
        List<CitationVO> citations = List.of();
        boolean useRag = request.getKnowledgeBaseId() != null;

        if (useRag) {
            RagAugmentation augmentation = augmentWithKnowledgeBase(request);
            systemPrompt = augmentation.systemPrompt();
            userPrompt = augmentation.promptPreview() != null ? augmentation.promptPreview() : userPrompt;
            citations = augmentation.citations();
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

    private CitationVO toCitation(RetrievalHit hit, IntentDispatchRequest request) {
        CitationVO citation = new CitationVO();
        citation.setDocumentName(hit.getDocumentName());
        citation.setPageNo(hit.getPageNo());
        citation.setChunkId(hit.getChunkId());
        citation.setScore(hit.getScore());
        citation.setExcerpt(hit.getExcerpt());
        citation.setChunkIndex(hit.getChunkIndex());
        citation.setLessonChapterId(request.getLessonChapterId());
        citation.setAnchor(hit.getHeading());
        citation.setDocumentId(hit.getDocumentId());
        citation.setKnowledgeBaseId(request.getKnowledgeBaseId());
        return citation;
    }

    private List<CitationVO> toCitations(RagResult ragResult, IntentDispatchRequest request) {
        if (ragResult == null || ragResult.getRetrievalResults() == null) {
            return List.of();
        }
        return ragResult.getRetrievalResults().stream()
                .map(hit -> toCitation(hit, request))
                .collect(Collectors.toList());
    }

    private RagAugmentation augmentWithKnowledgeBase(IntentDispatchRequest request) {
        knowledgeAccessApi.assertAccessible(request.getKnowledgeBaseId());
        String retrievalQuery = StringUtils.hasText(request.getRetrievalQuery())
                ? request.getRetrievalQuery()
                : request.getMessage();
        QueryRewriteContext rewriteContext = QueryRewriteContext.builder()
                .question(retrievalQuery)
                .courseName(resolveCourseName(request))
                .conversationHistory(request.getConversationHistory())
                .build();
        RagResult ragResult = executeLessonScopedRag(request, retrievalQuery, rewriteContext);
        Map<String, String> vars = new HashMap<>();
        vars.put("context", ragResult.getContext() != null ? ragResult.getContext() : "");
        String questionForTemplate = StringUtils.hasText(request.getRetrievalQuery())
                ? request.getRetrievalQuery()
                : request.getMessage();
        vars.put("question", questionForTemplate);
        String systemPrompt = promptService.renderTemplate("chat_rag", vars);
        if (StringUtils.hasText(request.getLessonEnrichmentBlock())) {
            systemPrompt = systemPrompt + "\n\n" + request.getLessonEnrichmentBlock().trim();
        }
        return new RagAugmentation(systemPrompt, ragResult.getPromptPreview(), toCitations(ragResult, request));
    }

    private RagResult executeLessonScopedRag(IntentDispatchRequest request, String retrievalQuery,
                                               QueryRewriteContext rewriteContext) {
        Long lessonDocId = request.getLessonDocumentId() != null
                ? request.getLessonDocumentId()
                : request.getDocumentId();
        if (lessonDocId != null) {
            double minScore = ragProperties.getMinRrfScore();
            RagResult scoped = ragPipeline.executeDetailed(
                    retrievalQuery,
                    request.getKnowledgeBaseId(),
                    5,
                    minScore,
                    lessonDocId,
                    false,
                    rewriteContext
            );
            if (scoped.getRetrievalResults() != null && !scoped.getRetrievalResults().isEmpty()
                    && scoped.getRetrievalResults().get(0).getScore() >= minScore) {
                return scoped;
            }
        }
        return ragPipeline.executeDetailed(
                retrievalQuery,
                request.getKnowledgeBaseId(),
                5,
                ragProperties.getMinRrfScore(),
                request.getDocumentId(),
                false,
                rewriteContext
        );
    }

    private record RagAugmentation(String systemPrompt, String promptPreview, List<CitationVO> citations) {
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
