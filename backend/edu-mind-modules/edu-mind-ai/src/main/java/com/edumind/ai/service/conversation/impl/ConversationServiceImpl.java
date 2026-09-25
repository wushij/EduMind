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
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    /** 取不到模型时的兜底标题长度（历史值是「提问前 20 字」，又长又不像标题） */
    private static final int SHORT_TITLE_CHARS = 12;
    /** 模型返回标题的硬性裁剪长度 */
    private static final int MAX_TITLE_CHARS = 14;
    private static final int TITLE_TIMEOUT_SECONDS = 6;

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
        return doGenerateTitle(entity, firstUserPrompt(conversationId));
    }

    @Override
    public void generateTitleIfAutoDerivedAsync(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        // 异步线程没有请求上下文：必须把租户/用户带过去，否则 DAO 的租户隔离取不到租户
        final LoginUser currentUser = UserContext.get();
        final Long tenantId = TenantContext.getTenantId();
        CompletableFuture.runAsync(() -> {
            if (tenantId != null && tenantId > 0) {
                TenantContext.setTenantId(tenantId);
            }
            if (currentUser != null) {
                UserContext.set(currentUser);
            }
            try {
                ConversationEntity entity = conversationDao.findById(conversationId);
                if (entity == null) {
                    return;
                }
                String firstUser = firstUserPrompt(conversationId);
                if (!isAutoDerivedTitle(entity.getTitle(), firstUser)) {
                    return;
                }
                doGenerateTitle(entity, firstUser);
            } catch (Exception ex) {
                log.warn("Async title generation failed conv={}: {}", conversationId, ex.getMessage());
            } finally {
                UserContext.clear();
                TenantContext.clear();
            }
        });
    }

    private String doGenerateTitle(ConversationEntity entity, String firstUserPrompt) {
        String cleanPrompt = firstUserPrompt == null
                ? ""
                : firstUserPrompt.replaceAll("^\\[[^\\]]+\\]\\s*", "").trim();
        String rawFallback = StringUtils.hasText(cleanPrompt) ? cleanPrompt : "新问答会话";
        String fallbackTitle = rawFallback.length() <= SHORT_TITLE_CHARS
                ? rawFallback
                : rawFallback.substring(0, SHORT_TITLE_CHARS);
        try {
            String modelKey = modelRouter.resolveModelKey("CHAT", null);
            AiCallAuditContext auditContext = AiCallAuditContext.builder()
                    .userId(entity.getUserId())
                    .tenantId(entity.getTenantId())
                    .courseId(entity.getCourseId())
                    .conversationId(entity.getId())
                    .build();
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> aiGatewayFacade.chat(
                    "CHAT_TITLE",
                    modelKey,
                    "你是会话标题生成器。只输出标题本身：不超过10个汉字，不带标点、引号、书名号，不要任何解释。",
                    cleanPrompt,
                    auditContext
            ));
            String title = cleanTitle(future.get(TITLE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            if (StringUtils.hasText(title)) {
                entity.setTitle(title);
                conversationDao.updateById(entity);
                return title;
            }
        } catch (Exception ex) {
            log.warn("Failed to generate title via LLM for conversation {}, fallback to user prompt: {}",
                    entity.getId(), ex.getMessage());
        }
        entity.setTitle(fallbackTitle);
        conversationDao.updateById(entity);
        return fallbackTitle;
    }

    /** 模型偶尔仍会带书名号 / 引号 / 换行或「标题：」前缀，统一清掉并按标题长度裁剪 */
    private String cleanTitle(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String cleaned = raw.replaceAll("[\\r\\n]+", " ")
                .replaceAll("[\"'“”‘’《》「」【】]", "")
                .replaceAll("^(标题|会话标题)\\s*[:：]?", "")
                .trim();
        return cleaned.length() > MAX_TITLE_CHARS ? cleaned.substring(0, MAX_TITLE_CHARS) : cleaned;
    }

    private String firstUserPrompt(String conversationId) {
        return messageDao.listByConversationId(conversationId).stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .map(MessageEntity::getContent)
                .findFirst()
                .orElse("");
    }

    /**
     * 标题是否仍由系统自动生成：默认标题，或就是首条提问的前缀（历史行为）。
     * 用户手动改过名的一律不再覆盖。
     */
    private boolean isAutoDerivedTitle(String title, String firstUserPrompt) {
        if (!StringUtils.hasText(title)) {
            return true;
        }
        String trimmed = title.trim();
        if ("新问答会话".equals(trimmed) || "新会话".equals(trimmed)) {
            return true;
        }
        String cleanPrompt = firstUserPrompt == null
                ? ""
                : firstUserPrompt.replaceAll("^\\[[^\\]]+\\]\\s*", "").trim();
        return StringUtils.hasText(cleanPrompt) && cleanPrompt.startsWith(trimmed);
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
