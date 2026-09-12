package com.edumind.ai.rag.pipeline;

import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.rag.context.ContextBuilder;
import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.rag.query.QueryRewriter;
import com.edumind.ai.rag.rerank.ScoreReranker;
import com.edumind.ai.rag.retrieval.RagRetrieverImpl;
import com.edumind.ai.service.prompt.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagPipelineImpl implements RagPipeline {

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
        String rewritten = queryRewriter.rewrite(query);
        List<RetrievalHit> hits = ragRetriever.retrieveWithRewrittenQuery(
                rewritten, knowledgeBaseId, topK, minScore, documentId);
        List<RetrievalHit> ranked = scoreReranker.rerank(hits, topK, minScore);
        String context = contextBuilder.build(ranked);
        Map<String, String> vars = new HashMap<>();
        vars.put("context", context);
        vars.put("question", query);
        String promptPreview = promptService.renderTemplate("chat_rag", vars);
        String answer = null;
        if (!skipLlm) {
            answer = aiGatewayFacade.chat("RAG", promptService.getSystemPrompt("chat"), promptPreview);
        }
        return RagResult.builder()
                .originalQuery(query)
                .rewrittenQuery(rewritten)
                .retrievalResults(ranked)
                .context(context)
                .promptPreview(promptPreview)
                .answer(answer)
                .build();
    }
}
