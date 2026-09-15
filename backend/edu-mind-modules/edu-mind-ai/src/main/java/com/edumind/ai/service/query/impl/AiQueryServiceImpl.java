package com.edumind.ai.service.query.impl;

import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.service.query.AiQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiQueryServiceImpl implements AiQueryService {

    private final ConversationDao conversationDao;

    @Override
    public long countConversations() {
        return conversationDao.countAll();
    }
}
