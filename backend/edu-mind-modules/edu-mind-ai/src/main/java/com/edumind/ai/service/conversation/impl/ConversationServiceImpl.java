package com.edumind.ai.service.conversation.impl;

import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.dao.MessageDao;
import com.edumind.ai.dto.ConversationCreateDTO;
import com.edumind.ai.dto.ConversationRenameDTO;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.service.conversation.ConversationService;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.vo.ConversationVO;
import com.edumind.ai.vo.MessageVO;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final AiConverter aiConverter;
    private final AiSessionCacheService aiSessionCacheService;
    private final LlmClient llmClient;

    @Override
    public List<ConversationVO> listConversations(Long courseId) {
        Long userId = requireUserId();
        return conversationDao.findByUserId(userId, courseId).stream()
                .map(aiConverter::toConversationVO)
                .collect(Collectors.toList());
    }

    @Override
    public ConversationVO createConversation(ConversationCreateDTO dto) {
        Long userId = requireUserId();
        ConversationEntity entity = new ConversationEntity();
        entity.setUserId(userId);
        entity.setCourseId(dto.getCourseId());
        entity.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : "新会话");
        entity.setMessageCount(0);
        entity.setTotalTokens(0);
        conversationDao.insert(entity);
        return aiConverter.toConversationVO(entity);
    }

    @Override
    public List<MessageVO> listMessages(String conversationId) {
        assertConversationOwner(conversationId);
        return messageDao.listByConversationId(conversationId).stream()
                .map(aiConverter::toMessageVO)
                .collect(Collectors.toList());
    }

    @Override
    public void renameConversation(String conversationId, ConversationRenameDTO dto) {
        ConversationEntity entity = assertConversationOwner(conversationId);
        entity.setTitle(dto.getTitle());
        conversationDao.updateById(entity);
    }

    @Override
    public void deleteConversation(String conversationId) {
        assertConversationOwner(conversationId);
        conversationDao.softDeleteById(conversationId);
        aiSessionCacheService.deleteSession(conversationId);
    }

    @Override
    public String generateTitle(String conversationId) {
        ConversationEntity entity = assertConversationOwner(conversationId);
        List<MessageEntity> messages = messageDao.listByConversationId(conversationId);
        String firstUser = messages.stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .map(MessageEntity::getContent)
                .findFirst()
                .orElse("新会话");
        String title = llmClient.chat(
                "你是会话标题生成器，请用不超过12个字概括用户问题。",
                firstUser
        );
        if (StringUtils.hasText(title)) {
            entity.setTitle(title.trim().replace("\"", ""));
            conversationDao.updateById(entity);
        }
        return entity.getTitle();
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        return userId;
    }

    private ConversationEntity assertConversationOwner(String conversationId) {
        ConversationEntity entity = conversationDao.findById(conversationId);
        if (entity == null) {
            throw new BusinessException("会话不存在");
        }
        Long userId = UserContext.getUserId();
        if (userId == null || !userId.equals(entity.getUserId())) {
            throw new BusinessException("无权访问该会话");
        }
        return entity;
    }
}
