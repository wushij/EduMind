package com.edumind.ai.service.conversation.impl;

import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.dao.MessageDao;
import com.edumind.ai.dto.ConversationCreateDTO;
import com.edumind.ai.dto.ConversationRenameDTO;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.gateway.ModelRouter;
import com.edumind.ai.service.conversation.ConversationService;
import com.edumind.ai.vo.ConversationVO;
import com.edumind.ai.vo.MessageVO;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final AiConverter aiConverter;
    private final AiSessionCacheService aiSessionCacheService;
    private final AiGatewayFacade aiGatewayFacade;
    private final ModelRouter modelRouter;

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
        // 仅软删会话与缓存；ai_call_log 为独立审计流水，删除聊天记录不影响 Token 统计
        conversationDao.softDeleteById(conversationId);
        aiSessionCacheService.deleteSession(conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> deleteMessageWithPair(String messageId) {
        MessageEntity target = messageDao.findById(messageId);
        if (target == null) {
            throw new BusinessException("消息不存在");
        }
        ConversationEntity conversation = assertConversationOwner(target.getConversationId());

        List<MessageEntity> messages = messageDao.listByConversationId(target.getConversationId());
        int targetIndex = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getId())) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex < 0) {
            throw new BusinessException("消息不存在");
        }

        List<String> idsToDelete = collectPairedMessageIds(messages, targetIndex);
        for (String id : idsToDelete) {
            messageDao.deleteById(id);
        }

        int currentCount = conversation.getMessageCount() != null ? conversation.getMessageCount() : 0;
        conversation.setMessageCount(Math.max(0, currentCount - idsToDelete.size()));
        conversationDao.updateById(conversation);
        aiSessionCacheService.deleteSession(target.getConversationId());
        return idsToDelete;
    }

    private List<String> collectPairedMessageIds(List<MessageEntity> messages, int targetIndex) {
        List<String> ids = new ArrayList<>();
        MessageEntity current = messages.get(targetIndex);
        ids.add(current.getId());

        if ("user".equalsIgnoreCase(current.getRole())) {
            if (targetIndex + 1 < messages.size()
                    && "assistant".equalsIgnoreCase(messages.get(targetIndex + 1).getRole())) {
                ids.add(messages.get(targetIndex + 1).getId());
            }
        } else if ("assistant".equalsIgnoreCase(current.getRole())) {
            if (targetIndex > 0 && "user".equalsIgnoreCase(messages.get(targetIndex - 1).getRole())) {
                ids.add(messages.get(targetIndex - 1).getId());
            }
        }
        return ids;
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
        String modelKey = modelRouter.resolveModelKey("CHAT", null);
        AiCallAuditContext auditContext = AiCallAuditContext.builder()
                .userId(LoginUserResolver.resolveUserId())
                .tenantId(TenantContext.getTenantId())
                .courseId(entity.getCourseId())
                .conversationId(conversationId)
                .build();
        String title = aiGatewayFacade.chat(
                "CHAT_TITLE",
                modelKey,
                "你是会话标题生成器，请用不超过12个字概括用户问题。",
                firstUser,
                auditContext
        );
        if (StringUtils.hasText(title)) {
            entity.setTitle(title.trim().replace("\"", ""));
            conversationDao.updateById(entity);
        }
        return entity.getTitle();
    }

    private Long requireUserId() {
        return LoginUserResolver.requireUserId();
    }

    private ConversationEntity assertConversationOwner(String conversationId) {
        ConversationEntity entity = conversationDao.findById(conversationId);
        if (entity == null) {
            throw new BusinessException("会话不存在");
        }
        Long userId = LoginUserResolver.resolveUserId();
        if (userId == null || !userId.equals(entity.getUserId())) {
            throw new BusinessException("无权访问该会话");
        }
        return entity;
    }
}
