package com.edumind.ai.rag.pipeline;

import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmChatOptions;
import com.edumind.ai.integration.llm.TokenEstimator;
import com.edumind.ai.rag.context.ContextBuilder;
import com.edumind.ai.rag.model.RagDebugOptions;
import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.model.RagStageTiming;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.rag.query.QueryRewriter;
import com.edumind.ai.rag.query.QueryRewriteContext;
import com.edumind.ai.rag.rerank.ScoreReranker;
import com.edumind.ai.rag.retrieval.RagRetrieverImpl;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.prompt.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagPipelineImpl implements RagPipeline {

    /** 阶段编码与前端 PipelineStageTiming.stage 一一对应 */
    private static final String STAGE_QUERY_REWRITE = "query_rewrite";
    private static final String STAGE_VECTOR_SEARCH = "vector_search";
    private static final String STAGE_RERANK = "rerank";
    private static final String STAGE_CONTEXT_ASSEMBLY = "context_assembly";
    private static final String STAGE_LLM_GENERATION = "llm_generation";

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_FAILED = "FAILED";

    /** 诊断场景码，与网关路由/AI 审计中的 RAG 场景保持一致 */
    private static final String SCENE = "RAG";

    private final QueryRewriter queryRewriter;
    private final RagRetrieverImpl ragRetriever;
    private final ScoreReranker scoreReranker;
    private final ContextBuilder contextBuilder;
    private final PromptService promptService;
    private final AiGatewayFacade aiGatewayFacade;

    @Override
    public String execute(String query, Long knowledgeBaseId) {
        return executeDetailed(query, knowledgeBaseId, 5, 0.0, null, false).getAnswer();
    }

    public RagResult executeDetailed(String query, Long knowledgeBaseId, int topK, double minScore,
                                     Long documentId, boolean skipLlm) {
        return executeDetailed(query, knowledgeBaseId, topK, minScore, documentId, skipLlm,
                QueryRewriteContext.ofQuestion(query));
    }

    public RagResult executeDetailed(String query, Long knowledgeBaseId, int topK, double minScore,
                                     Long documentId, boolean skipLlm, QueryRewriteContext rewriteContext) {
        return executeDetailed(query, knowledgeBaseId, topK, minScore, documentId, skipLlm, rewriteContext,
                RagDebugOptions.none());
    }

    /**
     * 带诊断选项的全链路执行。
     *
     * <p>相比默认链路，这里额外做两件事：</p>
     * <ol>
     *   <li>允许诊断工作台覆盖模型 / 温度 / 系统提示词（均为空时行为与默认链路完全一致）；</li>
     *   <li>为每个阶段记录真实耗时与摘要，并估算 Token 用量，供诊断面板展示。</li>
     * </ol>
     */
    public RagResult executeDetailed(String query, Long knowledgeBaseId, int topK, double minScore,
                                     Long documentId, boolean skipLlm, QueryRewriteContext rewriteContext,
                                     RagDebugOptions debugOptions) {
        RagDebugOptions options = debugOptions != null ? debugOptions : RagDebugOptions.none();
        long pipelineStart = System.nanoTime();
        List<RagStageTiming> stageTimings = new ArrayList<>(5);

        long stageStart = System.nanoTime();
        String rewritten = queryRewriter.rewrite(rewriteContext);
        stageTimings.add(stage(STAGE_QUERY_REWRITE, "Query 改写", stageStart,
                STATUS_SUCCESS,
                StringUtils.hasText(rewritten) && !rewritten.equals(query)
                        ? "改写为：" + abbreviate(rewritten, 60)
                        : "未改写，使用原始问题"));

        stageStart = System.nanoTime();
        List<RetrievalHit> hits = ragRetriever.retrieveWithRewrittenQuery(
                rewritten, knowledgeBaseId, topK, minScore, documentId);
        stageTimings.add(stage(STAGE_VECTOR_SEARCH, "向量 + 关键词混合检索", stageStart,
                STATUS_SUCCESS,
                "召回候选切片 " + hits.size() + " 条"));

        stageStart = System.nanoTime();
        List<RetrievalHit> ranked = scoreReranker.rerank(hits, topK, minScore);
        stageTimings.add(stage(STAGE_RERANK, "重排与阈值过滤", stageStart,
                STATUS_SUCCESS,
                "保留 " + ranked.size() + " 条（topK=" + topK + "，minScore=" + minScore + "）"));

        stageStart = System.nanoTime();
        String context = contextBuilder.build(ranked);
        stageTimings.add(stage(STAGE_CONTEXT_ASSEMBLY, "上下文拼装", stageStart,
                STATUS_SUCCESS,
                "拼装上下文 " + (context != null ? context.length() : 0) + " 字符"));

        Map<String, String> vars = new HashMap<>();
        vars.put("context", context);
        vars.put("question", query);
        String promptPreview = promptService.renderTemplate("chat_rag", vars);
        // 跳过生成时不解析系统提示词，保持与「仅检索」链路完全一致的调用行为
        String systemPrompt = skipLlm ? null : resolveSystemPrompt(options);

        String answer = null;
        String usedModelKey = null;
        stageStart = System.nanoTime();
        if (skipLlm) {
            stageTimings.add(stage(STAGE_LLM_GENERATION, "LLM 生成", stageStart, STATUS_SKIPPED, "本次诊断已跳过模型生成"));
        } else {
            try {
                // 检索增强问答依托具体知识库：带上 knowledgeBaseId 与命中数，
                // 该次调用才能经由「知识库 → 课程」归属到对应课程的 AI 消耗明细
                AiCallAuditContext auditContext = AiCallAuditContext.builder()
                        .knowledgeBaseId(knowledgeBaseId)
                        .retrievalHitCount(ranked.size())
                        .build();
                AiGatewayFacade.ChatOutcome outcome = aiGatewayFacade.chatWithMeta(
                        SCENE, options.modelKey(), systemPrompt, promptPreview,
                        buildChatOptions(options), auditContext);
                usedModelKey = outcome.modelKey();
                answer = outcome.content();
                stageTimings.add(stage(STAGE_LLM_GENERATION, "LLM 生成", stageStart, STATUS_SUCCESS,
                        "模型 " + usedModelKey + " 生成 " + (answer != null ? answer.length() : 0) + " 字符"));
            } catch (RuntimeException ex) {
                // 失败也要留下时序，便于在错误日志中定位是哪一阶段出问题
                stageTimings.add(stage(STAGE_LLM_GENERATION, "LLM 生成", stageStart, STATUS_FAILED,
                        "调用失败：" + abbreviate(ex.getMessage(), 80)));
                throw ex;
            }
        }

        int promptTokens = skipLlm
                ? TokenEstimator.estimate(promptPreview)
                : TokenEstimator.estimate(promptText(systemPrompt, promptPreview));
        int completionTokens = TokenEstimator.estimate(answer);

        return RagResult.builder()
                .originalQuery(query)
                .rewrittenQuery(rewritten)
                .retrievalResults(ranked)
                .context(context)
                .promptPreview(promptPreview)
                .answer(answer)
                .stageTimings(stageTimings)
                .modelKey(usedModelKey)
                .systemPromptOverridden(options.effectiveSystemPrompt() != null)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .totalLatencyMs(elapsedMs(pipelineStart))
                .build();
    }

    /** 系统提示词：诊断覆盖优先，其次平台默认 chat 系统提示词 */
    private String resolveSystemPrompt(RagDebugOptions options) {
        String override = options.effectiveSystemPrompt();
        return override != null ? override : promptService.getSystemPrompt("chat");
    }

    /** 计入 Prompt Token 的完整报文（系统提示词 + 组装后的用户 Prompt） */
    private String promptText(String systemPrompt, String promptPreview) {
        return (systemPrompt != null ? systemPrompt : "") + "\n" + (promptPreview != null ? promptPreview : "");
    }

    /** 仅覆盖温度时构造 options，否则返回 null 走 SDK 默认重载，避免改变既有参数行为 */
    private LlmChatOptions buildChatOptions(RagDebugOptions options) {
        return options.hasTemperatureOverride()
                ? LlmChatOptions.of(options.effectiveTemperature(), null)
                : null;
    }

    private RagStageTiming stage(String stage, String stageName, long stageStartNanos, String status, String summary) {
        return RagStageTiming.builder()
                .stage(stage)
                .stageName(stageName)
                .durationMs(elapsedMs(stageStartNanos))
                .status(status)
                .summary(summary)
                .build();
    }

    private long elapsedMs(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "…";
    }
}
