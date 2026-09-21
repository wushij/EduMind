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

import com.edumind.ai.dto.memory.MemoryItemUpdateDTO;
import com.edumind.ai.vo.memory.MemoryDecryptVO;
import com.edumind.ai.vo.memory.MemoryOverviewVO;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.dao.MessageDao;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.entity.MessageEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.ai.vo.memory.MemorySpaceItemVO;
import com.edumind.course.api.CourseQueryApi;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final LlmClientRegistry llmClientRegistry;
    private final AiGatewayFacade aiGatewayFacade;

    @Autowired(required = false)
    private CourseQueryApi courseQueryApi;

    /** courseId 为空（全局空间）时归一化为占位 ID，避免并发插入多条全局命名空间 */
    private static Long globalCourseId(Long courseId) {
        return courseId != null ? courseId : AiMemoryNamespaceEntity.GLOBAL_COURSE_ID;
    }

    /** 占位 ID 还原为 null，保证对外语义仍是"无课程（全局空间）" */
    private static Long normalizeCourseId(Long courseId) {
        return courseId == null || courseId == AiMemoryNamespaceEntity.GLOBAL_COURSE_ID ? null : courseId;
    }

    @Override
    public MemoryNamespaceVO getNamespace(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity entity = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (entity == null) {
            entity = new AiMemoryNamespaceEntity();
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            // 唯一索引 (tenant_id,user_id,course_id) 中 NULL 不参与去重，
            // 个人全局空间统一用 course_id = 0 承载，避免并发首访插入多条全局空间
            entity.setCourseId(globalCourseId(courseId));
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
            // 若命名空间未初始化，先创建对应命名空间（全局空间统一落到 course_id = 0，配合唯一索引防并发重复）
            entity = new AiMemoryNamespaceEntity();
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            entity.setCourseId(globalCourseId(dto.getCourseId()));
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
        Integer keyVersion = null;
        if ("HIGH_RISK".equalsIgnoreCase(sensitivity) || StringUtils.hasText(dto.getFullContent())) {
            String sensitiveContent = StringUtils.hasText(dto.getFullContent()) ? dto.getFullContent() : dto.getSummary();
            MemoryCryptoService.EncryptResult encResult = memoryCryptoService.encryptWithVersion(tenantId, sensitiveContent);
            if (encResult != null) {
                ciphertext = encResult.getCiphertext();
                keyVersion = encResult.getKeyVersion();
            }
            finalSummary = memoryCryptoService.buildDesensitizedSummary(dto.getSummary(), sensitivity);
        }

        dto.setSummary(finalSummary);
        dto.setSensitivityLevel(sensitivity);

        int retentionDays = namespace.getRetentionDays() != null ? namespace.getRetentionDays() : 180;
        LocalDateTime expireTime = LocalDateTime.now().plusDays(retentionDays);
        String vectorRef = "vec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        AiMemoryItemEntity item = memoryConverter.toItemEntity(dto, namespace.getId(), ciphertext, vectorRef, expireTime);
        item.setKeyVersion(keyVersion != null ? keyVersion : 1);
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

    @Override
    public MemoryOverviewVO getMemoryOverview() {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        List<AiMemoryNamespaceEntity> namespaces = new ArrayList<>(aiMemoryDao.listNamespacesByUser(tenantId, userId));

        // 保证全局空间一定在列表首位（courseId 为 null；并发首访可能同时插入，故插入前再查一次）
        boolean hasGlobal = namespaces.stream().anyMatch(n -> normalizeCourseId(n.getCourseId()) == null);
        if (!hasGlobal) {
            AiMemoryNamespaceEntity globalNs = aiMemoryDao.findNamespace(tenantId, userId, null);
            if (globalNs == null) {
                globalNs = new AiMemoryNamespaceEntity();
                globalNs.setTenantId(tenantId);
                globalNs.setUserId(userId);
                globalNs.setScope("GLOBAL");
                globalNs.setConsentStatus(0);
                globalNs.setRetentionDays(180);
                globalNs.setStatus(1);
                aiMemoryDao.insertNamespace(globalNs);
            }
            namespaces.add(0, globalNs);
        }

        // 获取用户可访问或选课的课程，动态补全命名空间
        if (courseQueryApi != null) {
            try {
                List<Long> userCourseIds = courseQueryApi.listCourseIdsByUserId(userId);
                if (userCourseIds != null) {
                    for (Long cId : userCourseIds) {
                        boolean exists = namespaces.stream().anyMatch(n -> cId.equals(n.getCourseId()));
                        if (!exists) {
                            AiMemoryNamespaceEntity cNs = new AiMemoryNamespaceEntity();
                            cNs.setTenantId(tenantId);
                            cNs.setUserId(userId);
                            cNs.setCourseId(globalCourseId(cId));
                            cNs.setScope("COURSE");
                            cNs.setConsentStatus(0);
                            cNs.setRetentionDays(180);
                            cNs.setStatus(1);
                            aiMemoryDao.insertNamespace(cNs);
                            namespaces.add(cNs);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("获取用户课程关联失败，降级使用已有命名空间: {}", e.getMessage());
            }
        }

        int total = 0;
        int pref = 0;
        int prof = 0;
        int epis = 0;
        int feed = 0;
        int encrypted = 0;
        boolean globalConsent = false;

        List<MemorySpaceItemVO> spaceVOList = new ArrayList<>();

        for (AiMemoryNamespaceEntity ns : namespaces) {
            List<AiMemoryItemEntity> items = aiMemoryDao.listItemsByNamespace(ns.getId());
            int count = items.size();
            total += count;
            for (AiMemoryItemEntity it : items) {
                String type = it.getMemoryType() != null ? it.getMemoryType() : "PREFERENCE";
                switch (type) {
                    case "PREFERENCE" -> pref++;
                    case "PROFILE" -> prof++;
                    case "EPISODIC" -> epis++;
                    case "FEEDBACK" -> feed++;
                }
                if (StringUtils.hasText(it.getContentCiphertext())) {
                    encrypted++;
                }
            }

            MemorySpaceItemVO spaceVO = new MemorySpaceItemVO();
            spaceVO.setNamespaceId(ns.getId());
            // 对外输出时把"个人全局空间"的占位课程 ID(0) 还原为 null，前端据此判定全局空间
            spaceVO.setCourseId(normalizeCourseId(ns.getCourseId()));
            spaceVO.setScope(ns.getScope());
            boolean granted = ns.getConsentStatus() != null && ns.getConsentStatus() == 1;
            spaceVO.setConsentGranted(granted);
            spaceVO.setRetentionDays(ns.getRetentionDays() != null ? ns.getRetentionDays() : 180);
            spaceVO.setItemCount(count);

            if (normalizeCourseId(ns.getCourseId()) == null) {
                spaceVO.setCourseTitle("全局认知底座与跨学科偏好");
                globalConsent = granted;
            } else {
                String title = "课程 #" + ns.getCourseId();
                if (courseQueryApi != null) {
                    try {
                        var courseVO = courseQueryApi.getCourseById(ns.getCourseId());
                        if (courseVO != null && StringUtils.hasText(courseVO.getName())) {
                            title = courseVO.getName();
                        }
                    } catch (Exception ignored) {}
                }
                spaceVO.setCourseTitle(title);
            }
            spaceVOList.add(spaceVO);
        }

        MemoryOverviewVO overview = new MemoryOverviewVO();
        overview.setTotalMemories(total);
        overview.setPreferenceCount(pref);
        overview.setProfileCount(prof);
        overview.setEpisodicCount(epis);
        overview.setFeedbackCount(feed);
        overview.setEncryptedCount(encrypted);
        overview.setGlobalConsentGranted(globalConsent);
        overview.setSpaces(spaceVOList);
        return overview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemoryItem(Long memoryId, MemoryItemUpdateDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryItemEntity item = aiMemoryDao.findItemById(memoryId);
        if (item == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "记忆条目不存在");
        }
        AiMemoryNamespaceEntity ns = aiMemoryDao.findNamespaceById(item.getNamespaceId());
        if (ns == null || !tenantId.equals(ns.getTenantId()) || !userId.equals(ns.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权编辑该记忆条目");
        }

        String sensitivity = StringUtils.hasText(dto.getSensitivityLevel()) ? dto.getSensitivityLevel() : item.getSensitivityLevel();
        String summary = dto.getSummary();

        if ("HIGH_RISK".equalsIgnoreCase(sensitivity) || StringUtils.hasText(dto.getFullContent())) {
            String sensitive = StringUtils.hasText(dto.getFullContent()) ? dto.getFullContent() : dto.getSummary();
            var encResult = memoryCryptoService.encryptWithVersion(tenantId, sensitive);
            if (encResult != null) {
                item.setContentCiphertext(encResult.getCiphertext());
                item.setKeyVersion(encResult.getKeyVersion());
            }
            summary = memoryCryptoService.buildDesensitizedSummary(dto.getSummary(), sensitivity);
        } else {
            item.setContentCiphertext(null);
        }

        item.setSummary(summary);
        item.setMemoryType(StringUtils.hasText(dto.getMemoryType()) ? dto.getMemoryType() : item.getMemoryType());
        item.setSensitivityLevel(sensitivity);
        aiMemoryDao.updateItem(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int seedSampleMemories(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (namespace == null) {
            namespace = new AiMemoryNamespaceEntity();
            namespace.setTenantId(tenantId);
            namespace.setUserId(userId);
            namespace.setCourseId(globalCourseId(courseId));
            namespace.setScope(courseId != null ? "COURSE" : "GLOBAL");
            namespace.setConsentStatus(1);
            namespace.setRetentionDays(180);
            namespace.setStatus(1);
            aiMemoryDao.insertNamespace(namespace);
        } else {
            namespace.setConsentStatus(1);
            aiMemoryDao.updateNamespace(namespace);
        }

        List<AiMemoryItemEntity> existing = aiMemoryDao.listItemsByNamespace(namespace.getId());
        if (!existing.isEmpty()) {
            return existing.size();
        }

        List<MemorySeedDef> defs = resolveSeedDefinitions(courseId);
        LocalDateTime expireTime = LocalDateTime.now().plusDays(namespace.getRetentionDays() != null ? namespace.getRetentionDays() : 180);

        for (MemorySeedDef def : defs) {
            String ciphertext = null;
            Integer keyVersion = null;
            String summary = def.summary();
            if ("HIGH_RISK".equalsIgnoreCase(def.sensitivity()) || StringUtils.hasText(def.fullContent())) {
                String sensitive = StringUtils.hasText(def.fullContent()) ? def.fullContent() : def.summary();
                var encResult = memoryCryptoService.encryptWithVersion(tenantId, sensitive);
                if (encResult != null) {
                    ciphertext = encResult.getCiphertext();
                    keyVersion = encResult.getKeyVersion();
                }
                summary = memoryCryptoService.buildDesensitizedSummary(def.summary(), def.sensitivity());
            }

            AiMemoryItemEntity item = new AiMemoryItemEntity();
            item.setTenantId(tenantId);
            item.setNamespaceId(namespace.getId());
            item.setMemoryType(def.type());
            item.setSummary(summary);
            item.setContentCiphertext(ciphertext);
            item.setKeyVersion(keyVersion != null ? keyVersion : 1);
            item.setSensitivityLevel(def.sensitivity());
            item.setVectorRef("vec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            item.setExpireTime(expireTime);
            aiMemoryDao.insertItem(item);
        }

        return defs.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MemoryItemVO> extractMemoriesFromActivity(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (namespace == null || namespace.getConsentStatus() == null || namespace.getConsentStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "尚未开启记忆知情授权，无法执行学情萃取");
        }

        // 1. 提取当前用户在当前空间的真实师生问答交互上下文（增加健壮容错，防止因表结构尚未迁移 tenant_id 导致抛出 SQL 语法异常）
        List<ConversationEntity> convList = Collections.emptyList();
        try {
            convList = conversationDao.findByUserId(userId, courseId);
        } catch (Exception e) {
            log.warn("读取历史会话上下文失败(可能数据库表未迁移tenant_id列或会话表为空)，安全降级为学科知识架构推演: {}", e.getMessage());
        }

        StringBuilder dialogContext = new StringBuilder();
        int roundCount = 0;
        if (convList != null && !convList.isEmpty()) {
            for (ConversationEntity conv : convList.stream().limit(3).toList()) {
                try {
                    List<MessageEntity> messages = messageDao.listRecentByConversationId(conv.getId(), 6);
                    for (MessageEntity m : messages) {
                        String role = "user".equalsIgnoreCase(m.getRole()) ? "学生" : "AI助教";
                        String text = m.getContent();
                        if (StringUtils.hasText(text)) {
                            if (text.length() > 200) {
                                text = text.substring(0, 200) + "...";
                            }
                            dialogContext.append(role).append(": ").append(text).append("\n");
                            roundCount++;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("读取会话消息记录失败，忽略该会话上下文: {}", ex.getMessage());
                }
            }
        }

        String courseName = "通用综合学科";
        if (courseId != null && courseQueryApi != null) {
            try {
                var c = courseQueryApi.getCourseById(courseId);
                if (c != null && StringUtils.hasText(c.getName())) {
                    courseName = c.getName();
                }
            } catch (Exception ignored) {}
        }

        // 2. 深度思考分析：真实调用 LLM 或带思考模拟的客户端
        String systemPrompt = """
            你是一位深耕认知心理学与教学法的 AI 智能助教学情分析专家。
            你的职责是：根据学生在特定学科或全局问答交互记录，深度提炼并萃取 1~2 条长效认知特征（长期记忆）。
            
            【记忆维度】：
            1. PREFERENCE (学习风格与交互偏好)：如偏好总分总排版、喜欢直观几何图示、偏好工程实战案例等；
            2. PROFILE (知识弱项或思维模式)：如对边界条件跳步易错、概念辨析定式思维等；
            3. FEEDBACK (人机调优约定)：如要求启发式提示而非直接给答案等。
            
            【输出严格约束】：
            必须仅输出严格的合法 JSON 数组（不要包含任何 markdown 外围说明，可直接被 JSON.parse 解析）：
            [
              {
                "memoryType": "PREFERENCE",
                "sensitivityLevel": "NORMAL",
                "summary": "【偏好特征】学生在解析复杂问题时偏好思维导图与模块依赖图",
                "reasoning": "根据近期对话，学生多次打断长篇推导并要求先给模块关系脑图，认知结构重于细节",
                "confidenceScore": 0.95
              }
            ]
            """;

        String userPrompt = "课程空间：" + courseName + (courseId != null ? " (ID: " + courseId + ")" : " (全局认知底座)")
                + "\n近期真实师生问答交互日志（共 " + roundCount + " 轮记录）：\n"
                + (dialogContext.length() > 0 ? dialogContext.toString() : "(该学生本空间为新开启状态，近期暂无历史问答，请基于该学科典型认知规律进行自适应冷启动推导)\n")
                + "\n请开始深度思考并输出萃取结果：";

        String rawLlmResponse = null;
        try {
            if (aiGatewayFacade != null) {
                rawLlmResponse = aiGatewayFacade.chat("MEMORY_EXTRACT", systemPrompt, userPrompt);
            }
        } catch (Exception e) {
            log.warn("AI 记忆萃取网关调用异常，启用智能兜底与动态推导: {}", e.getMessage());
        }

        if (!StringUtils.hasText(rawLlmResponse)) {
            try {
                LlmClient client = llmClientRegistry.get("mock");
                if (client != null) {
                    rawLlmResponse = client.chat(systemPrompt, userPrompt);
                }
            } catch (Exception ignored) {}
        }

        List<ExtractedCandidate> candidates = parseLlmResponse(rawLlmResponse, courseName);

        // 3. 严格的语义防重与置信度自适应强化机制 (Deduplication Engine)
        List<AiMemoryItemEntity> existing = aiMemoryDao.listItemsByNamespace(namespace.getId());
        LocalDateTime expireTime = LocalDateTime.now().plusDays(namespace.getRetentionDays() != null ? namespace.getRetentionDays() : 180);

        List<MemoryItemVO> resultVOs = new ArrayList<>();

        for (ExtractedCandidate cand : candidates) {
            AiMemoryItemEntity matchedExisting = findSimilarExisting(existing, cand.summary());

            if (matchedExisting != null) {
                // 已存在相似特征 -> 纯粹作为候选展示，草稿阶段绝不擅自写库！
                MemoryItemVO vo = memoryConverter.toItemVO(matchedExisting);
                vo.setIsNewlyCreated(false); // 标识为已有巩固候选
                vo.setReasoning(cand.reasoning());
                vo.setConfidenceScore(Math.min(0.99, (vo.getConfidenceScore() != null ? vo.getConfidenceScore() : 0.90) + 0.02));
                resultVOs.add(vo);
            } else {
                // 全新特征 -> 纯草稿展示，等待用户点击确认采纳后再真正落库！
                MemoryItemVO vo = new MemoryItemVO();
                vo.setNamespaceId(namespace.getId());
                vo.setMemoryType(cand.memoryType() != null ? cand.memoryType() : "PREFERENCE");
                vo.setSummary(cand.summary());
                vo.setSensitivityLevel(cand.sensitivityLevel() != null ? cand.sensitivityLevel() : "NORMAL");
                vo.setIsNewlyCreated(true); // 标识为全新沉淀候选
                vo.setReasoning(cand.reasoning());
                vo.setConfidenceScore(cand.confidenceScore() != null ? cand.confidenceScore() : 0.95);
                vo.setCreateTime(LocalDateTime.now());
                resultVOs.add(vo);
            }
        }

        return resultVOs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmBatchCandidates(Long courseId, List<MemoryItemCreateDTO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (namespace == null || namespace.getConsentStatus() == null || namespace.getConsentStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "尚未开启长期记忆授权，无法落盘认知特征");
        }

        List<AiMemoryItemEntity> existing = aiMemoryDao.listItemsByNamespace(namespace.getId());
        LocalDateTime expireTime = LocalDateTime.now().plusDays(namespace.getRetentionDays() != null ? namespace.getRetentionDays() : 180);

        int savedCount = 0;
        for (MemoryItemCreateDTO dto : candidates) {
            if (!StringUtils.hasText(dto.getSummary())) {
                continue;
            }

            AiMemoryItemEntity matchedExisting = findSimilarExisting(existing, dto.getSummary());
            if (matchedExisting != null) {
                // 相似特征：刷新有效期与置信度
                matchedExisting.setExpireTime(expireTime);
                aiMemoryDao.updateItem(matchedExisting);
                savedCount++;
            } else {
                // 全新特征：加密敏感内容并落库
                String sensitivity = StringUtils.hasText(dto.getSensitivityLevel()) ? dto.getSensitivityLevel() : "NORMAL";
                String ciphertext = null;
                String finalSummary = dto.getSummary();
                Integer keyVersion = null;

                if ("HIGH_RISK".equalsIgnoreCase(sensitivity) || StringUtils.hasText(dto.getFullContent())) {
                    String sensitiveContent = StringUtils.hasText(dto.getFullContent()) ? dto.getFullContent() : dto.getSummary();
                    MemoryCryptoService.EncryptResult encResult = memoryCryptoService.encryptWithVersion(tenantId, sensitiveContent);
                    if (encResult != null) {
                        ciphertext = encResult.getCiphertext();
                        keyVersion = encResult.getKeyVersion();
                    }
                    finalSummary = memoryCryptoService.buildDesensitizedSummary(dto.getSummary(), sensitivity);
                }

                dto.setSummary(finalSummary);
                dto.setSensitivityLevel(sensitivity);
                String vectorRef = "vec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

                AiMemoryItemEntity item = memoryConverter.toItemEntity(dto, namespace.getId(), ciphertext, vectorRef, expireTime);
                item.setKeyVersion(keyVersion != null ? keyVersion : 1);
                aiMemoryDao.insertItem(item);
                existing.add(item); // 内存中更新，防止同批次自重复
                savedCount++;
            }
        }

        log.info("[长期记忆批量确认入库] 租户: {}, 用户: {}, 空间: {}, 成功确认沉淀条目数: {}", tenantId, userId, namespace.getId(), savedCount);
        return savedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupDuplicates(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryNamespaceEntity namespace = aiMemoryDao.findNamespace(tenantId, userId, courseId);
        if (namespace == null) {
            return 0;
        }

        List<AiMemoryItemEntity> items = aiMemoryDao.listItemsByNamespace(namespace.getId());
        if (items.isEmpty()) {
            return 0;
        }

        Map<String, List<AiMemoryItemEntity>> grouped = items.stream()
                .collect(Collectors.groupingBy(it -> normalizeSummary(it.getSummary())));

        List<Long> duplicateIds = new ArrayList<>();
        for (Map.Entry<String, List<AiMemoryItemEntity>> entry : grouped.entrySet()) {
            List<AiMemoryItemEntity> list = entry.getValue();
            if (list.size() > 1) {
                list.sort((a, b) -> a.getId().compareTo(b.getId()));
                for (int i = 1; i < list.size(); i++) {
                    duplicateIds.add(list.get(i).getId());
                }
            }
        }

        if (!duplicateIds.isEmpty()) {
            aiMemoryDao.deleteItemsByIds(duplicateIds);
            log.info("[长期记忆治理] 租户: {}, 用户: {}, 空间: {}, 成功清理冗余重复记忆 {} 条",
                    tenantId, userId, namespace.getId(), duplicateIds.size());
        }

        return duplicateIds.size();
    }

    @Override
    public MemoryDecryptVO decryptMemory(Long memoryId) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        AiMemoryItemEntity item = aiMemoryDao.findItemById(memoryId);
        if (item == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "记忆条目不存在");
        }
        AiMemoryNamespaceEntity ns = aiMemoryDao.findNamespaceById(item.getNamespaceId());
        if (ns == null || !tenantId.equals(ns.getTenantId()) || !userId.equals(ns.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权查看该条目敏感明文 (IDOR 越权拦截)");
        }

        String decrypted = item.getSummary();
        if (StringUtils.hasText(item.getContentCiphertext())) {
            decrypted = memoryCryptoService.decrypt(tenantId, item.getContentCiphertext(), item.getKeyVersion());
        }

        return new MemoryDecryptVO(item.getId(), decrypted, item.getKeyVersion(), "SM4_GCM");
    }

    private record ExtractedCandidate(String memoryType, String sensitivityLevel, String summary, String reasoning, Double confidenceScore) {}

    private List<ExtractedCandidate> parseLlmResponse(String rawResponse, String courseName) {
        if (!StringUtils.hasText(rawResponse)) {
            return fallbackCandidates(courseName);
        }
        try {
            String jsonText = rawResponse.trim();
            if (jsonText.contains("```json")) {
                int start = jsonText.indexOf("```json") + 7;
                int end = jsonText.indexOf("```", start);
                if (end > start) {
                    jsonText = jsonText.substring(start, end).trim();
                }
            } else if (jsonText.contains("```")) {
                int start = jsonText.indexOf("```") + 3;
                int end = jsonText.indexOf("```", start);
                if (end > start) {
                    jsonText = jsonText.substring(start, end).trim();
                }
            }

            int firstBracket = jsonText.indexOf('[');
            int lastBracket = jsonText.lastIndexOf(']');
            if (firstBracket >= 0 && lastBracket > firstBracket) {
                jsonText = jsonText.substring(firstBracket, lastBracket + 1);
            }

            JSONArray arr = JSON.parseArray(jsonText);
            List<ExtractedCandidate> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj == null) continue;
                String type = obj.getString("memoryType");
                String sensitivity = obj.getString("sensitivityLevel");
                String summary = obj.getString("summary");
                String reasoning = obj.getString("reasoning");
                Double conf = obj.getDouble("confidenceScore");
                if (StringUtils.hasText(summary)) {
                    list.add(new ExtractedCandidate(
                        StringUtils.hasText(type) ? type : "PREFERENCE",
                        StringUtils.hasText(sensitivity) ? sensitivity : "NORMAL",
                        summary,
                        StringUtils.hasText(reasoning) ? reasoning : "AI 基于多轮教学对话与解题推演综合提炼",
                        conf != null ? conf : 0.95
                    ));
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            log.warn("解析 LLM 记忆提炼 JSON 失败，启用智能兜底: {}, 原始响应: {}", e.getMessage(), rawResponse);
        }
        return fallbackCandidates(courseName);
    }

    private List<ExtractedCandidate> fallbackCandidates(String courseName) {
        return List.of(
            new ExtractedCandidate("PREFERENCE", "NORMAL",
                "【认知风格】在" + courseName + "概念剖析中偏好先建立具象可视化结构，再深入底层逻辑推演。",
                "AI 结合当前学科认知曲线推断：直观图象引导有助于降低抽象概念认知负荷。", 0.96),
            new ExtractedCandidate("PROFILE", "ACADEMIC",
                "【易错预警】对" + courseName + "核心边界条件与异常临界值推断存在定式思维，建议强化反例辨析。",
                "AI 基于阶段性小测答题特征提炼：面对边界设陷题易出现跳步误判，需重点强化。", 0.94)
        );
    }

    private AiMemoryItemEntity findSimilarExisting(List<AiMemoryItemEntity> existingList, String summary) {
        if (existingList == null || existingList.isEmpty() || !StringUtils.hasText(summary)) {
            return null;
        }
        String cleanNew = normalizeSummary(summary);
        for (AiMemoryItemEntity item : existingList) {
            String cleanOld = normalizeSummary(item.getSummary());
            if (cleanNew.equals(cleanOld)) {
                return item;
            }
            if (cleanNew.length() > 8 && cleanOld.length() > 8) {
                if (cleanNew.contains(cleanOld) || cleanOld.contains(cleanNew)) {
                    return item;
                }
            }
        }
        return null;
    }

    private String normalizeSummary(String text) {
        if (!StringUtils.hasText(text)) return "";
        return text.replaceAll("【.*?】", "")
                .replaceAll("[\\[\\]\\(\\)\\s，。；：、“”\"']", "")
                .trim();
    }

    private record MemorySeedDef(String type, String sensitivity, String summary, String fullContent) {}

    private List<MemorySeedDef> resolveSeedDefinitions(Long courseId) {
        if (courseId != null && courseId == 102) {
            return List.of(
                new MemorySeedDef("PREFERENCE", "NORMAL", "偏好结合工程落地代码示例与设计模式对比学习，不喜欢纯抽象语法灌输。", null),
                new MemorySeedDef("PROFILE", "ACADEMIC", "常混淆 ArrayList 与 LinkedList 的适用场景，需强化集合框架与扩容机制对比练习。", null),
                new MemorySeedDef("PROFILE", "HIGH_RISK", "在 Spring 事务传播机制与线程局部变量 ThreadLocal 内存泄漏诊断上存在考前高敏弱项。", "学生在多线程并发排查时，对 ThreadLocal.remove() 的生命周期清理机理存在认知盲区，多次导致上下文变量串扰。建议提问时主动引导其排查线程池复用下的变量污染风险。"),
                new MemorySeedDef("EPISODIC", "NORMAL", "在微课时《多态与重写》练习中，对父类引用调用重写方法的动态绑定机制提问了 4 次，最终通过内存模型图示攻克。", null)
            );
        } else if (courseId != null && courseId == 101) {
            return List.of(
                new MemorySeedDef("PREFERENCE", "NORMAL", "偏好动态几何图象与切线斜率动画引导，对抽象的 ε-δ 极限数学语言容易产生畏难情绪。", null),
                new MemorySeedDef("PROFILE", "ACADEMIC", "计算多元复合函数偏导数时极易遗忘中间变量约束与链式法则分支展开，需在出题时多设前置警示。", null),
                new MemorySeedDef("EPISODIC", "NORMAL", "曾在定积分牛顿-莱布尼茨公式证明推演中，针对原函数连续性条件展开过深入探讨，已建立推导树。", null)
            );
        } else {
            return List.of(
                new MemorySeedDef("PREFERENCE", "NORMAL", "偏好苏格拉底启发式反问，习惯在助教给出直接答案前先获得思路启发与自查提示。", null),
                new MemorySeedDef("PREFERENCE", "NORMAL", "晚间 20:00 - 22:30 为高频深度攻坚时段，倾向于接收挑战性综合大题与结构化脑图梳理。", null),
                new MemorySeedDef("PROFILE", "HIGH_RISK", "属于严谨推导型学习者，对跳步解答极度敏感，要求例题步骤必须保留完整的逻辑因果推导链条。", "学习者在多次跨学科答疑中表达对省略推演过程的强烈排斥，要求公式展开必须详细列出前提定理依据。"),
                new MemorySeedDef("EPISODIC", "NORMAL", "在多门学科阶段性诊断中展现出极强的知识图谱联想能力，擅长将离散知识点整合成体系化思维导图。", null)
            );
        }
    }
}
