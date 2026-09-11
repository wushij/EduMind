package com.edumind.ai.api.impl;

import com.edumind.ai.api.AiQueryApi;
import com.edumind.ai.dao.ConversationDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiQueryApiImpl implements AiQueryApi {

    private final ConversationDao conversationDao;

    @Override
    public long countConversations() {
        return conversationDao.countAll();
    }
}
