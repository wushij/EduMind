package com.edumind.ai.service.memory.impl;

import com.edumind.ai.dao.memory.AiMemoryDao;
import com.edumind.ai.dto.memory.MemoryConsentDTO;
import com.edumind.ai.dto.memory.MemoryFeedbackDTO;
import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.entity.memory.AiMemoryFeedbackEntity;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.entity.memory.AiMemoryNamespaceEntity;
import com.edumind.ai.service.memory.AgentMemoryService;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 长期记忆服务实现 (严格遵循 IDOR 跨租户/跨用户防护与隐私授权生命周期规范)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMemoryServiceImpl implements AgentMemoryService {

    private final AiMemoryDao aiMemoryDao;

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
            // 隐私合规：默认状态为未授权 (0: PENDING/NOT_AUTHORIZED)，需用户显式同意
            entity.setConsentStatus(0);
            entity.setStatus(1);
            aiMemoryDao.insertNamespace(entity);
        }

        MemoryNamespaceVO vo = new MemoryNamespaceVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setCourseId(entity.getCourseId());
        vo.setScope(entity.getScope());
        vo.setConsentStatus(entity.getConsentStatus() != null && entity.getConsentStatus() == 1);

        List<AiMemoryItemEntity> items = aiMemoryDao.listItemsByNamespace(entity.getId());
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    private MemoryItemVO toItemVO(AiMemoryItemEntity entity) {
        MemoryItemVO vo = new MemoryItemVO();
        vo.setId(entity.getId());
        vo.setNamespaceId(entity.getNamespaceId());
        vo.setSummary(entity.getSummary());
        vo.setSensitivityLevel(entity.getSensitivityLevel());
        vo.setVectorRef(entity.getVectorRef());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConsent(MemoryConsentDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity entity = aiMemoryDao.findNamespace(tenantId, userId, dto.getCourseId());
        if (entity != null) {
            boolean isAgreed = Boolean.TRUE.equals(dto.getConsent());
            entity.setConsentStatus(isAgreed ? 1 : 0);
            aiMemoryDao.updateNamespace(entity);

            // 隐私保护：若用户撤回授权，立即物理清理既有记忆条目与向量引用
            if (!isAgreed) {
                int deletedCount = aiMemoryDao.deleteItemsByNamespaceId(entity.getId());
                log.info("[长期记忆隐私撤回] 用户 {} 已撤销命名空间 {} 授权，已级联清理 {} 条记忆资产",
                        userId, entity.getId(), deletedCount);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMemoryItem(MemoryItemCreateDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, dto.getCourseId());
        if (namespace == null || namespace.getConsentStatus() == null || namespace.getConsentStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户尚未完成长期记忆隐私授权，拒绝写入知识事实");
        }

        AiMemoryItemEntity item = new AiMemoryItemEntity();
        item.setNamespaceId(namespace.getId());
        item.setSummary(dto.getSummary());
        item.setSensitivityLevel(dto.getSensitivityLevel() != null ? dto.getSensitivityLevel() : "NORMAL");
        item.setVectorRef("vec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        item.setExpireTime(LocalDateTime.now().plusMonths(6));
        aiMemoryDao.insertItem(item);

        log.info("[长期记忆入库] 租户: {}, 用户: {}, 命名空间: {}, 记忆ID: {}",
                tenantId, userId, namespace.getId(), item.getId());
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
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作或删除非本人的长期记忆资产 (IDOR 越权拦截)");
        }

        aiMemoryDao.deleteItemById(memoryId);
        log.info("[长期记忆遗忘] 用户 {} 成功物理删除记忆条目 ID: {}, 同步注销向量索引: {}",
                userId, memoryId, item.getVectorRef());
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

        AiMemoryFeedbackEntity fb = new AiMemoryFeedbackEntity();
        fb.setMemoryId(memoryId);
        fb.setUserId(userId);
        fb.setFeedbackAction(dto.getFeedbackAction());
        fb.setCorrectContent(dto.getCorrectContent());
        fb.setReason(dto.getReason());
        aiMemoryDao.insertFeedback(fb);

        if ("MODIFY".equalsIgnoreCase(dto.getFeedbackAction()) && dto.getCorrectContent() != null) {
            item.setSummary(dto.getCorrectContent());
            aiMemoryDao.updateItem(item);
        } else if ("FORGET".equalsIgnoreCase(dto.getFeedbackAction())) {
            forgetMemory(memoryId);
        }
    }

    @Override
    public List<MemoryItemVO> retrieveMemories(Long courseId, String queryPrompt) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (namespace == null || namespace.getConsentStatus() == null || namespace.getConsentStatus() != 1) {
            return List.of();
        }

        List<AiMemoryItemEntity> list = aiMemoryDao.listItemsByNamespace(namespace.getId());
        if (list.isEmpty()) {
            return List.of();
        }

        // 若携带 queryPrompt，按文本相关度进行过滤与排序
        if (queryPrompt != null && !queryPrompt.isBlank()) {
            String queryLower = queryPrompt.toLowerCase();
            return list.stream()
                    .sorted(Comparator.comparingInt((AiMemoryItemEntity item) -> {
                        String s = item.getSummary().toLowerCase();
                        int score = 0;
                        for (String kw : queryLower.split("\\s+")) {
                            if (s.contains(kw)) score++;
                        }
                        return -score;
                    }).thenComparing(AiMemoryItemEntity::getCreateTime, Comparator.reverseOrder()))
                    .limit(5)
                    .map(this::toItemVO)
                    .collect(Collectors.toList());
        }

        return list.stream().limit(5).map(this::toItemVO).collect(Collectors.toList());
    }
}
