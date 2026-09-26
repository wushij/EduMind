package com.edumind.ai.rag.rerank;

import com.edumind.ai.rag.model.RetrievalHit;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScoreReranker {

    public List<RetrievalHit> rerank(List<RetrievalHit> hits, int topK, double minScore) {
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        List<RetrievalHit> filtered = hits.stream()
                .filter(hit -> hit.getScore() >= minScore)
                .sorted(Comparator.comparing(RetrievalHit::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            // 如果严格阈值导致全部滤空，兜底保留原始排序最靠前的候选，防止上下文断流
            return hits.stream()
                    .sorted(Comparator.comparing(RetrievalHit::getScore).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());
        }
        return filtered;
    }
}
