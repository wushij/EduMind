package com.edumind.ai.api.impl;

import com.edumind.ai.api.AiChatApi;
import com.edumind.ai.gateway.AiGatewayFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiChatApiImpl implements AiChatApi {

    private final AiGatewayFacade aiGatewayFacade;

    @Override
    public String chat(String scene, String systemPrompt, String userPrompt) {
        return aiGatewayFacade.chat(scene, systemPrompt, userPrompt);
    }
}
