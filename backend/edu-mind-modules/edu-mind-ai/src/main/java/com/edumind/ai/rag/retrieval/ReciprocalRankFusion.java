package com.edumind.ai.rag.retrieval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReciprocalRankFusion {

    private ReciprocalRankFusion() {
    }

    public static Map<Long, Double> fuse(Map<String, List<Long>> rankedLists, Map<String, Double> weights, double k) {
        Map<Long, Double> scores = new HashMap<>();
        if (rankedLists == null || rankedLists.isEmpty()) {
            return scores;
        }
        for (Map.Entry<String, List<Long>> entry : rankedLists.entrySet()) {
            double weight = weights != null && weights.containsKey(entry.getKey())
                    ? weights.get(entry.getKey())
                    : 1.0;
            List<Long> ids = entry.getValue();
            if (ids == null) {
                continue;
            }
            for (int rank = 0; rank < ids.size(); rank++) {
                Long id = ids.get(rank);
                if (id == null) {
                    continue;
                }
                scores.merge(id, weight / (k + rank + 1.0), Double::sum);
            }
        }
        return scores;
    }
}
