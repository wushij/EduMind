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

    /**
     * 选择基础系统提示词。
     *
     * <p>以下两种情形都不能沿用课程助教的 chat 口径，否则模型会凭通用知识编排平台并不具备的能力
     * （实测出现过「课堂互动 Agent」「口语评测」「家校沟通」这类本平台没有的内容）：</p>
     * <ol>
     *   <li><b>全域通用空间</b>：没有任何课程上下文可依；</li>
     *   <li><b>问的是平台自身能力与使用方式</b>：与当前课程无关，即便锚定了课程也必须按平台口径作答。</li>
     * </ol>
     * <p>这两种情形统一改用 global_assistant 模板，其中载明了平台真实能力清单与回答纪律。</p>
     */
    private String resolveBaseSystemPrompt(IntentDispatchRequest request) {
        IntentRouter.IntentResult intent = request.getIntent();
        boolean platformQuery = intent != null && "platform".equalsIgnoreCase(intent.targetCode());
        if (!request.isGlobalScope() && !platformQuery) {
            return promptService.getSystemPrompt("chat");
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("extraContext", "");
        return promptService.renderTemplate("global_assistant", vars);
    }

    private IntentDispatchPlan buildChatPlan(IntentDispatchRequest request, IntentRouter.IntentResult intent) {
        String systemPrompt = resolveBaseSystemPrompt(request);
        String userPrompt = request.getMessage();
        List<CitationVO> citations = List.of();
        boolean useRag = request.getKnowledgeBaseId() != null;

        if (useRag) {
            RagAugmentation augmentation = augmentWithKnowledgeBase(request);
            systemPrompt = augmentation.systemPrompt();
            userPrompt = augmentation.userPrompt();
            citations = augmentation.citations();
        } else if (StringUtils.hasText(request.getLessonEnrichmentBlock())) {
            systemPrompt = systemPrompt + "\n\n" + request.getLessonEnrichmentBlock().trim();
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
        String systemPrompt = resolveBaseSystemPrompt(request);
        String userPrompt = request.getMessage();
        List<CitationVO> citations = List.of();
        boolean useRag = request.getKnowledgeBaseId() != null;

        if (useRag) {
            RagAugmentation augmentation = augmentWithKnowledgeBase(request);
            systemPrompt = augmentation.systemPrompt();
            userPrompt = augmentation.userPrompt() != null ? augmentation.userPrompt() : userPrompt;
            citations = augmentation.citations();
        } else if (StringUtils.hasText(request.getLessonEnrichmentBlock())) {
            systemPrompt = systemPrompt + "\n\n" + request.getLessonEnrichmentBlock().trim();
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
        String context = ragResult.getContext() != null ? ragResult.getContext().trim() : "";
        Map<String, String> vars = new HashMap<>();
        vars.put("context", context);
        String questionForTemplate = StringUtils.hasText(request.getRetrievalQuery())
                ? request.getRetrievalQuery()
                : request.getMessage();
        vars.put("question", questionForTemplate);

        String systemPrompt = promptService.renderSystemPrompt("chat_rag", vars);
        if (!StringUtils.hasText(systemPrompt)) {
            systemPrompt = promptService.renderTemplate("chat_rag", vars);
        }
        if (!StringUtils.hasText(systemPrompt)) {
            systemPrompt = resolveBaseSystemPrompt(request);
        }
        if (StringUtils.hasText(request.getLessonEnrichmentBlock())) {
            systemPrompt = systemPrompt + "\n\n" + request.getLessonEnrichmentBlock().trim();
        }

        String userPrompt;
        if (StringUtils.hasText(context)) {
            String userContentTemplate = promptService.renderUserContent("chat_rag", vars);
            if (StringUtils.hasText(userContentTemplate) && userContentTemplate.contains(context)) {
                userPrompt = userContentTemplate;
            } else {
                userPrompt = "请结合以下课程参考资料，回答用户的学习问题。\n\n【参考资料】\n"
                        + context + "\n\n【用户问题】\n" + request.getMessage();
            }
        } else {
            userPrompt = request.getMessage();
        }
        return new RagAugmentation(systemPrompt, userPrompt, toCitations(ragResult, request));
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

    private record RagAugmentation(String systemPrompt, String userPrompt, List<CitationVO> citations) {
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
