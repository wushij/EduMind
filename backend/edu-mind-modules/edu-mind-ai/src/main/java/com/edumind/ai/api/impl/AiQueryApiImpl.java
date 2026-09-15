package com.edumind.ai.api.impl;

import com.edumind.ai.api.AiQueryApi;
import com.edumind.ai.service.query.AiQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiQueryApiImpl implements AiQueryApi {

    private final AiQueryService aiQueryService;

    @Override
    public long countConversations() {
        return aiQueryService.countConversations();
    }
}
