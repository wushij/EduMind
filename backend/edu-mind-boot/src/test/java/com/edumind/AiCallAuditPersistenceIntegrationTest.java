package com.edumind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.mapper.AiCallLogMapper;
import com.edumind.ai.mapper.ConversationMapper;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.audit.AiCallAuditService;
import com.edumind.ai.service.conversation.ConversationService;
import com.edumind.ai.service.usage.PersonalAiUsageService;
import com.edumind.ai.vo.usage.PersonalAiUsageVO;
import com.edumind.common.context.TenantContext;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.system.dao.SysTenantDao;
import com.edumind.system.entity.SysTenantEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 验证 ai_call_log 为独立审计流水：删除聊天记录后 Token 用量仍保留。
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class AiCallAuditPersistenceIntegrationTest {

    private static final Long TENANT_ID = 9951L;
    private static final Long USER_ID = 99511L;
    private static final Long COURSE_ID = 99501L;
    private static final String SCENE = "CHAT";

    @Autowired
    private AiCallAuditService aiCallAuditService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private PersonalAiUsageService personalAiUsageService;

    @Autowired
    private ConversationDao conversationDao;

    @Autowired
    private AiCallLogDao aiCallLogDao;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private AiCallLogMapper aiCallLogMapper;

    @Autowired
    private SysTenantDao sysTenantDao;

    @BeforeEach
    void setUp() {
        cleanData();
    }

    @AfterEach
    void tearDown() {
        cleanData();
        TenantContext.clear();
        UserContext.clear();
    }

    private void cleanData() {
        TenantContext.runWithoutTenant(() -> {
            ensureTenant(TENANT_ID, "TENANT_9951", "AI审计持久化测试学校");
            aiCallLogMapper.delete(new LambdaQueryWrapper<AiCallLogEntity>()
                    .eq(AiCallLogEntity::getUserId, USER_ID));
            conversationMapper.delete(new LambdaQueryWrapper<ConversationEntity>()
                    .eq(ConversationEntity::getUserId, USER_ID));
        });
    }

    private void ensureTenant(Long tenantId, String code, String name) {
        SysTenantEntity tenant = sysTenantDao.findById(tenantId);
        if (tenant == null) {
            tenant = new SysTenantEntity();
            tenant.setId(tenantId);
            tenant.setCode(code);
            tenant.setName(name);
            tenant.setStatus(1);
            tenant.setPlanCode("PRO");
            tenant.setCreateTime(LocalDateTime.now());
            tenant.setUpdateTime(LocalDateTime.now());
            sysTenantDao.insert(tenant);
        }
    }

    private void mockLogin() {
        TenantContext.setTenantId(TENANT_ID);
        UserContext.set(LoginUser.builder()
                .id(USER_ID)
                .username("audit_user")
                .realName("审计测试用户")
                .roles(List.of("STUDENT"))
                .permissions(List.of("ai:chat"))
                .build());
    }

    @Test
    @DisplayName("删除会话后 ai_call_log 仍保留，个人用量统计不受影响")
    void aiCallLogSurvivesConversationDeletion() {
        mockLogin();

        ConversationEntity conversation = new ConversationEntity();
        conversation.setUserId(USER_ID);
        conversation.setTenantId(TENANT_ID);
        conversation.setCourseId(COURSE_ID);
        conversation.setTitle("待删除会话");
        conversation.setMessageCount(0);
        conversation.setTotalTokens(0);
        conversationDao.insert(conversation);

        AiCallAuditContext auditContext = AiCallAuditContext.builder()
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .courseId(COURSE_ID)
                .conversationId(conversation.getId())
                .build();
        aiCallAuditService.record(SCENE, null, auditContext, System.currentTimeMillis(), 120, 80);

        long logCountBeforeDelete = aiCallLogDao.count(new LambdaQueryWrapper<AiCallLogEntity>()
                .eq(AiCallLogEntity::getUserId, USER_ID)
                .eq(AiCallLogEntity::getConversationId, conversation.getId()));
        Assertions.assertEquals(1, logCountBeforeDelete, "AI 调用应先写入 ai_call_log");

        conversationService.deleteConversation(conversation.getId());
        Assertions.assertNull(conversationDao.findById(conversation.getId()), "会话应被软删除");

        long logCountAfterDelete = aiCallLogDao.count(new LambdaQueryWrapper<AiCallLogEntity>()
                .eq(AiCallLogEntity::getUserId, USER_ID)
                .eq(AiCallLogEntity::getConversationId, conversation.getId()));
        Assertions.assertEquals(1, logCountAfterDelete, "删除聊天记录不应删除 ai_call_log");

        PersonalAiUsageVO usage = personalAiUsageService.getMyUsage(7, 1, 10);
        Assertions.assertTrue(usage.getTodayTokensUsed() >= 200, "今日 Token 应包含已删会话的调用");
        Assertions.assertTrue(usage.getRecentLogs().stream()
                        .anyMatch(log -> SCENE.equals(log.getScene()) && log.getTotalTokens() >= 200),
                "个人用量明细应仍展示已删会话对应的调用记录");
    }
}
