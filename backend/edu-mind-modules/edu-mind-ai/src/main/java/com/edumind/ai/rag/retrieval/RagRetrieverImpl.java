package com.edumind.ai.rag.retrieval;

import com.edumind.ai.api.embedding.EmbeddingApi;
import com.edumind.ai.rag.config.RagProperties;
import com.edumind.ai.rag.model.RetrievalHit;
import com.edumind.ai.rag.query.QueryRewriter;
import com.edumind.common.context.TenantContext;
import com.edumind.infrastructure.vector.VectorSearchResult;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.config.MilvusProperties;
import com.edumind.knowledge.api.ChunkQueryApi;
import com.edumind.knowledge.api.ChunkRetrievalApi;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.ChunkKeywordSearchVO;
import com.edumind.knowledge.vo.knowledge.ChunkVO;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagRetrieverImpl implements RagRetriever {

    private static final int HYBRID_BRANCH_LIMIT = 20;

    private final QueryRewriter queryRewriter;
    private final EmbeddingApi embeddingApi;
    private final VectorStore vectorStore;
    private final MilvusProperties milvusProperties;
    private final ChunkQueryApi chunkQueryApi;
    private final ChunkRetrievalApi chunkRetrievalApi;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final RagProperties ragProperties;
    private final VectorRecallLatencyTracker latencyTracker;

    @Override
    public List<String> retrieve(String query, Long knowledgeBaseId, int topK, double minScore) {
        return retrieveHits(query, knowledgeBaseId, topK, minScore, null).stream()
                .map(RetrievalHit::getExcerpt)
                .collect(Collectors.toList());
    }

    public List<RetrievalHit> retrieveHits(String query, Long knowledgeBaseId, int topK, double minScore,
                                           Long documentId) {
        return retrieveWithRewrittenQuery(queryRewriter.rewrite(query), knowledgeBaseId, topK, minScore, documentId);
    }

    public List<RetrievalHit> retrieveWithRewrittenQuery(String rewritten, Long knowledgeBaseId, int topK,
                                                         double minScore, Long documentId) {
        if (!StringUtils.hasText(rewritten)) {
            return List.of();
        }
        if (ragProperties.isHybridEnabled()) {
            return hybridRetrieve(rewritten, knowledgeBaseId, topK, minScore, documentId);
        }
        return vectorOnlyRetrieve(rewritten, knowledgeBaseId, topK, minScore, documentId);
    }

    private List<RetrievalHit> hybridRetrieve(String rewritten, Long knowledgeBaseId, int topK, double minScore,
                                              Long documentId) {
        List<RetrievalHit> vectorHits = vectorOnlyRetrieve(
                rewritten, knowledgeBaseId, HYBRID_BRANCH_LIMIT, 0.0, documentId);
        List<Long> vectorIds = vectorHits.stream()
                .map(RetrievalHit::getChunkId)
                .filter(id -> id != null)
                .toList();

        ChunkKeywordSearchVO keyword = chunkRetrievalApi.searchKeywords(
                knowledgeBaseId, documentId, rewritten, HYBRID_BRANCH_LIMIT);

        Map<String, List<Long>> rankedLists = new LinkedHashMap<>();
        rankedLists.put("vector", vectorIds);
        rankedLists.put("phrase", safeList(keyword.getPhraseRankedChunkIds()));
        rankedLists.put("token", safeList(keyword.getTokenRankedChunkIds()));
        rankedLists.put("tech", safeList(keyword.getTechTermRankedChunkIds()));

        Map<String, Double> weights = new HashMap<>();
        weights.put("vector", 1.0);
        weights.put("phrase", 1.0);
        weights.put("token", 1.0);
        weights.put("tech", ragProperties.getTechTermBranchWeight());

        Map<Long, Double> fused = ReciprocalRankFusion.fuse(
                rankedLists, weights, ragProperties.getRrfK());

        double rrfFloor = minScore > 0 ? minScore : ragProperties.getMinRrfScore();
        List<Long> orderedIds = fused.entrySet().stream()
                .filter(e -> e.getValue() >= rrfFloor)
                .sorted(Comparator.comparing(Map.Entry<Long, Double>::getValue).reversed())
                .map(Map.Entry::getKey)
                .limit(topK)
                .toList();

        return buildHits(orderedIds, fused, knowledgeBaseId);
    }

    private List<RetrievalHit> vectorOnlyRetrieve(String rewritten, Long knowledgeBaseId, int topK,
                                                  double minScore, Long documentId) {
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
        long startTime = System.currentTimeMillis();
        List<VectorSearchResult> results = vectorStore.searchNearest(
                milvusProperties.getCollection(), queryVector, Math.max(topK * 2, topK), filter);
        long elapsedMs = System.currentTimeMillis() - startTime;
        if (latencyTracker != null) {
            latencyTracker.record(knowledgeBaseId, elapsedMs);
        }
        Map<Long, KnowledgeDocumentVO> documentMap = loadDocumentMap(knowledgeBaseId);
        List<RetrievalHit> hits = new ArrayList<>();
        for (VectorSearchResult result : results) {
            Long chunkId = Long.valueOf(result.getId());
            List<ChunkVO> chunks = chunkQueryApi.listByIds(List.of(chunkId));
            if (chunks.isEmpty()) {
                continue;
            }
            ChunkVO chunk = chunks.get(0);
            hits.add(toHit(chunk, documentMap.get(chunk.getDocumentId()), result.getScore(), result.getMetadata()));
        }
        return hits.stream()
                .filter(hit -> hit.getScore() >= minScore)
                .limit(topK)
                .collect(Collectors.toList());
    }

    private List<RetrievalHit> buildHits(List<Long> chunkIds, Map<Long, Double> scoreMap, Long knowledgeBaseId) {
        if (chunkIds.isEmpty()) {
            return List.of();
        }
        Map<Long, KnowledgeDocumentVO> documentMap = loadDocumentMap(knowledgeBaseId);
        List<ChunkVO> chunks = chunkQueryApi.listByIds(chunkIds);
        Map<Long, ChunkVO> chunkMap = chunks.stream()
                .collect(Collectors.toMap(ChunkVO::getId, c -> c, (a, b) -> a));
        List<RetrievalHit> hits = new ArrayList<>();
        for (Long chunkId : chunkIds) {
            ChunkVO chunk = chunkMap.get(chunkId);
            if (chunk == null) {
                continue;
            }
            double score = scoreMap.getOrDefault(chunkId, 0.0);
            hits.add(toHit(chunk, documentMap.get(chunk.getDocumentId()), score, Map.of()));
        }
        return hits;
    }

    private RetrievalHit toHit(ChunkVO chunk, KnowledgeDocumentVO document, double score,
                               Map<String, Object> metadata) {
        String excerpt = chunk.getContent();
        if (excerpt != null && excerpt.length() > 240) {
            excerpt = excerpt.substring(0, 240) + "...";
        }
        return RetrievalHit.builder()
                .chunkId(chunk.getId())
                .score(score)
                .documentId(chunk.getDocumentId())
                .documentName(document != null ? document.getFileName() : null)
                .pageNo(chunk.getPageNo())
                .heading(chunk.getHeading())
                .chunkIndex(chunk.getChunkIndex())
                .excerpt(excerpt)
                .metadata(metadata)
                .build();
    }

    private Map<Long, KnowledgeDocumentVO> loadDocumentMap(Long knowledgeBaseId) {
        Map<Long, KnowledgeDocumentVO> documentMap = new HashMap<>();
        for (KnowledgeDocumentVO doc : knowledgeQueryApi.listDocumentsByKnowledgeBaseId(knowledgeBaseId)) {
            documentMap.put(doc.getId(), doc);
        }
        return documentMap;
    }

    private List<Long> safeList(List<Long> ids) {
        return ids != null ? ids : List.of();
    }
}
