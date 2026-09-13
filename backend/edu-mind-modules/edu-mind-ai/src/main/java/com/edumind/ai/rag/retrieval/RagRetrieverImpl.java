package com.edumind.ai.rag.retrieval;

import com.edumind.common.api.embedding.EmbeddingApi;
import com.edumind.common.context.TenantContext;
import com.edumind.infrastructure.vector.VectorSearchResult;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.config.MilvusProperties;
import com.edumind.knowledge.api.ChunkQueryApi;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.ChunkVO;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.rag.query.QueryRewriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagRetrieverImpl implements RagRetriever {

    private final QueryRewriter queryRewriter;
    private final EmbeddingApi embeddingApi;
    private final VectorStore vectorStore;
    private final MilvusProperties milvusProperties;
    private final ChunkQueryApi chunkQueryApi;
    private final KnowledgeQueryApi knowledgeQueryApi;

    @Override
    public List<String> retrieve(String query, Long knowledgeBaseId, int topK, double minScore) {
        return retrieveHits(query, knowledgeBaseId, topK, minScore, null).stream()
                .map(hit -> hit.getExcerpt())
                .collect(Collectors.toList());
    }

    public List<RetrievalHit> retrieveHits(String query, Long knowledgeBaseId, int topK, double minScore, Long documentId) {
        return retrieveWithRewrittenQuery(queryRewriter.rewrite(query), knowledgeBaseId, topK, minScore, documentId);
    }

    public List<RetrievalHit> retrieveWithRewrittenQuery(String rewritten, Long knowledgeBaseId, int topK,
                                                         double minScore, Long documentId) {
        if (!StringUtils.hasText(rewritten)) {
            return List.of();
        }
        List<List<Float>> embeddings = embeddingApi.embed(List.of(rewritten));
        if (embeddings == null || embeddings.isEmpty()) {
            return List.of();
        }
        List<Float> queryVector = embeddings.get(0);
        Map<String, Object> filter = new HashMap<>();
        filter.put("knowledgeBaseId", knowledgeBaseId);
        if (documentId != null) {
            filter.put("documentId", documentId);
        }
        Long tenantId = TenantContext.requireTenantId();
        filter.put("tenantId", tenantId);
        List<VectorSearchResult> results = vectorStore.searchNearest(
                milvusProperties.getCollection(), queryVector, topK * 2, filter);
        Map<Long, KnowledgeDocumentVO> documentMap = new HashMap<>();
        for (KnowledgeDocumentVO doc : knowledgeQueryApi.listDocumentsByKnowledgeBaseId(knowledgeBaseId)) {
            documentMap.put(doc.getId(), doc);
        }
        List<RetrievalHit> hits = new ArrayList<>();
        for (VectorSearchResult result : results) {
            Long chunkId = Long.valueOf(result.getId());
            List<ChunkVO> chunks = chunkQueryApi.listByIds(List.of(chunkId));
            if (chunks.isEmpty()) {
                continue;
            }
            ChunkVO chunk = chunks.get(0);
            KnowledgeDocumentVO document = documentMap.get(chunk.getDocumentId());
            String excerpt = chunk.getContent();
            if (excerpt != null && excerpt.length() > 240) {
                excerpt = excerpt.substring(0, 240) + "...";
            }
            hits.add(RetrievalHit.builder()
                    .chunkId(chunk.getId())
                    .score(result.getScore())
                    .documentId(chunk.getDocumentId())
                    .documentName(document != null ? document.getFileName() : null)
                    .pageNo(chunk.getPageNo())
                    .heading(chunk.getHeading())
                    .chunkIndex(chunk.getChunkIndex())
                    .excerpt(excerpt)
                    .metadata(result.getMetadata())
                    .build());
        }
        return hits.stream()
                .filter(hit -> hit.getScore() >= minScore)
                .limit(topK)
                .collect(Collectors.toList());
    }
}
