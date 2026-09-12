package com.edumind.ai.rag.context;

import com.edumind.ai.rag.model.RetrievalHit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContextBuilder {

    public String build(List<RetrievalHit> hits) {
        return hits.stream()
                .map(hit -> String.format("[%s p.%s] %s",
                        hit.getDocumentName() != null ? hit.getDocumentName() : "doc",
                        hit.getPageNo() != null ? hit.getPageNo() : "-",
                        hit.getExcerpt()))
                .collect(Collectors.joining("\n\n"));
    }
}
