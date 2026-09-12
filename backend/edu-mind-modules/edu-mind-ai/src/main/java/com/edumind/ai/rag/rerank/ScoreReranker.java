package com.edumind.ai.rag.rerank;

import com.edumind.ai.rag.model.RetrievalHit;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScoreReranker {

    public List<RetrievalHit> rerank(List<RetrievalHit> hits, int topK, double minScore) {
        return hits.stream()
                .filter(hit -> hit.getScore() >= minScore)
                .sorted(Comparator.comparing(RetrievalHit::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }
}
