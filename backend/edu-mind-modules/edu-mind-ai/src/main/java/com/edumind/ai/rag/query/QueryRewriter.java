package com.edumind.ai.rag.query;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class QueryRewriter {

    public String rewrite(String query) {
        if (!StringUtils.hasText(query)) {
            return query;
        }
        return query.trim().replaceAll("\\s+", " ");
    }
}
