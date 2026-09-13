package com.edumind.ai.service.memory.impl;

import com.edumind.ai.converter.memory.MemoryConverter;
import com.edumind.ai.dao.memory.AiMemoryDao;
import com.edumind.ai.dto.memory.MemoryConsentDTO;
import com.edumind.ai.dto.memory.MemoryFeedbackDTO;
import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.entity.memory.AiMemoryFeedbackEntity;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.entity.memory.AiMemoryNamespaceEntity;
import com.edumind.ai.service.memory.AgentMemoryService;
import com.edumind.ai.service.memory.MemoryCryptoService;
import com.edumind.ai.service.memory.retrieval.MemoryContextBlock;
import com.edumind.ai.service.memory.retrieval.MemoryRetrievalService;
import com.edumind.ai.vo.memory.MemoryItemVO;
import com.edumind.ai.vo.memory.MemoryNamespaceVO;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 长期记忆服务实现 (遵循 AGENTS.md 分层、国密 SM4 敏感加密与严格 IDOR 隔离)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMemoryServiceImpl implements AgentMemoryService {

    private final AiMemoryDao aiMemoryDao;
    private final MemoryConverter memoryConverter;
    private final MemoryCryptoService memoryCryptoService;
    private final MemoryRetrievalService memoryRetrievalService;

    @Override
    public MemoryNamespaceVO getNamespace(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity entity = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (entity == null) {
            entity = new AiMemoryNamespaceEntity();
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            entity.setCourseId(courseId);
            entity.setScope(courseId != null ? "COURSE" : "GLOBAL");
            // 隐私合规首选：默认未授权 (0: PENDING/NOT_AUTHORIZED)，需用户显式知情同意
            entity.setConsentStatus(0);
            entity.setRetentionDays(180);
            entity.setStatus(1);
            aiMemoryDao.insertNamespace(entity);
        }

        List<AiMemoryItemEntity> items = aiMemoryDao.listItemsByNamespace(entity.getId());
        return memoryConverter.toNamespaceVO(entity, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConsent(MemoryConsentDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity entity = aiMemoryDao.findNamespace(tenantId, userId, dto.getCourseId());
        if (entity == null) {
            // 若命名空间未初始化，先创建对应命名空间
            entity = new AiMemoryNamespaceEntity();
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            entity.setCourseId(dto.getCourseId());
            entity.setScope(dto.getCourseId() != null ? "COURSE" : "GLOBAL");
            entity.setStatus(1);
            entity.setRetentionDays(dto.getRetentionDays() != null ? dto.getRetentionDays() : 180);
            entity.setConsentStatus(dto.isAgreed() ? 1 : 0);
            aiMemoryDao.insertNamespace(entity);
            return;
        }

        boolean isAgreed = dto.isAgreed();
        entity.setConsentStatus(isAgreed ? 1 : 0);
        if (dto.getRetentionDays() != null) {
            entity.setRetentionDays(dto.getRetentionDays());
        }
        aiMemoryDao.updateNamespace(entity);

        // 隐私保护核心机制：若用户撤回授权，立即物理清理全部既有记忆条目与向量引用 (Fail-Closed)
        if (!isAgreed) {
            int deletedCount = aiMemoryDao.deleteItemsByNamespaceId(entity.getId());
            log.info("[长期记忆隐私撤回] 租户 {} 用户 {} 命名空间 {} 已撤销授权，已级联擦除 {} 条记忆资产",
                    tenantId, userId, entity.getId(), deletedCount);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMemoryItem(MemoryItemCreateDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, dto.getCourseId());
        if (namespace == null || namespace.getConsentStatus() == null || namespace.getConsentStatus() != 1) {
            log.warn("[长期记忆写入拦截] 租户 {} 用户 {} 尝试在未获授权命名空间写入记忆", tenantId, userId);
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户尚未完成长期记忆隐私授权，拒绝写入知识事实");
        }

        String sensitivity = StringUtils.hasText(dto.getSensitivityLevel()) ? dto.getSensitivityLevel() : "NORMAL";
        String ciphertext = null;
        String finalSummary = dto.getSummary();

        // 国密 SM4 敏感信息加密处理：当标记为 HIGH_RISK 或携带 fullContent 时加密密文落盘
        if ("HIGH_RISK".equalsIgnoreCase(sensitivity) || StringUtils.hasText(dto.getFullContent())) {
            String sensitiveContent = StringUtils.hasText(dto.getFullContent()) ? dto.getFullContent() : dto.getSummary();
            ciphertext = memoryCryptoService.encrypt(tenantId, sensitiveContent);
            finalSummary = memoryCryptoService.buildDesensitizedSummary(dto.getSummary(), sensitivity);
        }

        dto.setSummary(finalSummary);
        dto.setSensitivityLevel(sensitivity);

        int retentionDays = namespace.getRetentionDays() != null ? namespace.getRetentionDays() : 180;
        LocalDateTime expireTime = LocalDateTime.now().plusDays(retentionDays);
        String vectorRef = "vec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        AiMemoryItemEntity item = memoryConverter.toItemEntity(dto, namespace.getId(), ciphertext, vectorRef, expireTime);
        aiMemoryDao.insertItem(item);

        log.info("[长期记忆入库] 租户: {}, 用户: {}, 命名空间: {}, 记忆ID: {}, 敏感级: {}, 是否加密: {}",
                tenantId, userId, namespace.getId(), item.getId(), sensitivity, ciphertext != null);
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgetMemory(Long memoryId) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryItemEntity item = aiMemoryDao.findItemById(memoryId);
        if (item == null) {
            return;
        }

        // 严格 IDOR 所有权校验：必须核实命名空间的租户与归属用户
        AiMemoryNamespaceEntity ns = aiMemoryDao.findNamespaceById(item.getNamespaceId());
        if (ns == null || !tenantId.equals(ns.getTenantId()) || !userId.equals(ns.getUserId())) {
            log.warn("[长期记忆越权拦截] 用户 {} (租户 {}) 企图删除条目 {} (归属租户 {}, 归属用户 {})",
                    userId, tenantId, memoryId, ns != null ? ns.getTenantId() : null, ns != null ? ns.getUserId() : null);
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作或删除非本人的长期记忆资产 (IDOR 越权拦截)");
        }

        aiMemoryDao.deleteItemById(memoryId);
        log.info("[长期记忆遗忘] 用户 {} 成功物理删除记忆条目 ID: {}, 同步注销向量引用: {}",
                userId, memoryId, item.getVectorRef());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgetAll(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (namespace != null) {
            int deletedCount = aiMemoryDao.deleteItemsByNamespaceId(namespace.getId());
            log.info("[长期记忆全部清空] 用户 {} 成功行使一键被遗忘权，擦除空间 {} 下的 {} 条条目",
                    userId, namespace.getId(), deletedCount);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void feedbackMemory(Long memoryId, MemoryFeedbackDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryItemEntity item = aiMemoryDao.findItemById(memoryId);
        if (item == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "记忆条目不存在");
        }

        // 严格 IDOR 校验
        AiMemoryNamespaceEntity ns = aiMemoryDao.findNamespaceById(item.getNamespaceId());
        if (ns == null || !tenantId.equals(ns.getTenantId()) || !userId.equals(ns.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权对他人记忆条目进行纠错反馈");
        }

        String action = dto.getFeedbackAction();
        if (!StringUtils.hasText(action)) {
            if (dto.getRelevanceScore() != null && dto.getRelevanceScore() <= 2) {
                action = "FORGET";
            } else {
                action = "MODIFY";
            }
        }

        AiMemoryFeedbackEntity fb = new AiMemoryFeedbackEntity();
        fb.setMemoryId(memoryId);
        fb.setUserId(userId);
        fb.setFeedbackAction(action);
        fb.setCorrectContent(dto.getCorrectContent());
        fb.setReason(dto.getReason());
        aiMemoryDao.insertFeedback(fb);

        if ("MODIFY".equalsIgnoreCase(action) && StringUtils.hasText(dto.getCorrectContent())) {
            item.setSummary(dto.getCorrectContent());
            aiMemoryDao.updateItem(item);
        } else if ("FORGET".equalsIgnoreCase(action)) {
            forgetMemory(memoryId);
        }
    }

    @Override
    public List<MemoryItemVO> retrieveMemories(Long courseId, String queryPrompt) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        List<MemoryContextBlock> blocks = memoryRetrievalService.retrieve(tenantId, userId, courseId, queryPrompt, 5);
        if (blocks.isEmpty()) {
            return Collections.emptyList();
        }

        return blocks.stream().map(b -> {
            MemoryItemVO vo = new MemoryItemVO();
            vo.setId(b.getId());
            vo.setSummary(b.getSummary());
            vo.setFullContent(b.getFullContent());
            vo.setMemoryType(b.getMemoryType());
            vo.setSensitivityLevel(b.getSensitivityLevel());
            vo.setConfidenceScore(b.getScore());
            vo.setEncrypted(b.getEncrypted());
            vo.setAccessCount(0);
            return vo;
        }).collect(Collectors.toList());
    }
}
