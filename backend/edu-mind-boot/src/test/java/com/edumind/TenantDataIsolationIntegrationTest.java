package com.edumind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.ConversationDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.ConversationEntity;
import com.edumind.ai.mapper.AiCallLogMapper;
import com.edumind.ai.mapper.ConversationMapper;
import com.edumind.common.context.TenantContext;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.mapper.CourseMapper;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.mapper.KnowledgeBaseMapper;
import com.edumind.question.dao.QuestionDao;
import com.edumind.question.entity.QuestionEntity;
import com.edumind.question.mapper.QuestionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

/**
 * 智教云 V2.0 · 核心五张表多租户物理数据行级隔离与跨校越权防御集成测试
 * (course, knowledge_base, edu_question, ai_conversation, ai_call_log)
 * 验证：A/B 校跨租户 IDOR 防护、平台超管旁路 (runWithoutTenant) 与 Fail-Closed (tenantId = -1) 机制
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TenantDataIsolationIntegrationTest {

    @Autowired
    private CourseDao courseDao;
    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private KnowledgeBaseDao knowledgeBaseDao;
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private QuestionDao questionDao;
    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ConversationDao conversationDao;
    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private AiCallLogDao aiCallLogDao;
    @Autowired
    private AiCallLogMapper aiCallLogMapper;

    @BeforeEach
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. [course] 验证课程表物理行级隔离：A校/B校互不可见与超管旁路")
    void testCourseRowLevelIsolation() {
        // 1. A 校 (tenantId = 1001) 上下文插入课程
        TenantContext.setTenantId(1001L);
        CourseEntity courseA = new CourseEntity();
        courseA.setTenantId(1001L);
        courseA.setCode("ISO_TEST_A_" + System.currentTimeMillis());
        courseA.setTitle("A校微积分隔离测试课");
        courseA.setTeacherId(9991L);
        courseA.setStatus(1);
        courseMapper.insert(courseA);
        Long courseAId = courseA.getId();
        Assertions.assertNotNull(courseAId);

        // 2. B 校 (tenantId = 1002) 上下文插入课程
        TenantContext.setTenantId(1002L);
        CourseEntity courseB = new CourseEntity();
        courseB.setTenantId(1002L);
        courseB.setCode("ISO_TEST_B_" + System.currentTimeMillis());
        courseB.setTitle("B校微积分隔离测试课");
        courseB.setTeacherId(9992L);
        courseB.setStatus(1);
        courseMapper.insert(courseB);
        Long courseBId = courseB.getId();
        Assertions.assertNotNull(courseBId);

        try {
            // 3. A 校上下文：只能查到自身课程，不能越权查到 B 校课程
            TenantContext.setTenantId(1001L);
            CourseEntity queryA = courseDao.findById(courseAId);
            Assertions.assertNotNull(queryA, "A校必须能查询到自身所属课程");
            Assertions.assertEquals("A校微积分隔离测试课", queryA.getTitle());

            CourseEntity crossQueryB = courseDao.findById(courseBId);
            Assertions.assertNull(crossQueryB, "A校绝不允许跨租户越权查询到B校课程(IDOR防护)");

            // 4. B 校上下文：只能查到自身课程，不能越权查到 A 校课程
            TenantContext.setTenantId(1002L);
            CourseEntity queryB = courseDao.findById(courseBId);
            Assertions.assertNotNull(queryB, "B校必须能查询到自身所属课程");

            CourseEntity crossQueryA = courseDao.findById(courseAId);
            Assertions.assertNull(crossQueryA, "B校绝不允许跨租户越权查询到A校课程(IDOR防护)");

            // 5. 平台运维旁路 runWithoutTenant：透传可查
            TenantContext.runWithoutTenant(() -> {
                CourseEntity bypassA = courseDao.findById(courseAId);
                CourseEntity bypassB = courseDao.findById(courseBId);
                Assertions.assertNotNull(bypassA, "超管旁路模式应能查询A校课程");
                Assertions.assertNotNull(bypassB, "超管旁路模式应能查询B校课程");
            });

            // 6. Fail-Closed (缺失租户时)：默认 tenantId = -1，查不到任何数据
            TenantContext.clear();
            CourseEntity failClosedA = courseDao.findById(courseAId);
            CourseEntity failClosedB = courseDao.findById(courseBId);
            Assertions.assertNull(failClosedA, "缺失租户上下文时必须触发 Fail-Closed，查询为空");
            Assertions.assertNull(failClosedB, "缺失租户上下文时必须触发 Fail-Closed，查询为空");

        } finally {
            TenantContext.runWithoutTenant(() -> {
                courseMapper.deleteById(courseAId);
                courseMapper.deleteById(courseBId);
            });
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("2. [knowledge_base] 验证知识库表物理行级隔离：A校/B校互不可见与超管旁路")
    void testKnowledgeBaseRowLevelIsolation() {
        // 1. A 校 (tenantId = 1001) 插入知识库
        TenantContext.setTenantId(1001L);
        KnowledgeBaseEntity kbA = new KnowledgeBaseEntity();
        kbA.setTenantId(1001L);
        kbA.setName("A校计算机网络知识库_" + System.currentTimeMillis());
        kbA.setDescription("A校专用网络协议");
        kbA.setStatus(1);
        knowledgeBaseMapper.insert(kbA);
        Long kbAId = kbA.getId();
        Assertions.assertNotNull(kbAId);

        // 2. B 校 (tenantId = 1002) 插入知识库
        TenantContext.setTenantId(1002L);
        KnowledgeBaseEntity kbB = new KnowledgeBaseEntity();
        kbB.setTenantId(1002L);
        kbB.setName("B校计算机网络知识库_" + System.currentTimeMillis());
        kbB.setDescription("B校专用网络协议");
        kbB.setStatus(1);
        knowledgeBaseMapper.insert(kbB);
        Long kbBId = kbB.getId();
        Assertions.assertNotNull(kbBId);

        try {
            // 3. A 校上下文校验
            TenantContext.setTenantId(1001L);
            KnowledgeBaseEntity queryA = knowledgeBaseDao.findById(kbAId);
            Assertions.assertNotNull(queryA, "A校必须能查询到自身知识库");
            Assertions.assertEquals(kbA.getName(), queryA.getName());

            KnowledgeBaseEntity crossB = knowledgeBaseDao.findById(kbBId);
            Assertions.assertNull(crossB, "A校绝不能越权查到B校知识库");

            // 4. B 校上下文校验
            TenantContext.setTenantId(1002L);
            KnowledgeBaseEntity queryB = knowledgeBaseDao.findById(kbBId);
            Assertions.assertNotNull(queryB, "B校必须能查询到自身知识库");

            KnowledgeBaseEntity crossA = knowledgeBaseDao.findById(kbAId);
            Assertions.assertNull(crossA, "B校绝不能越权查到A校知识库");

            // 5. 超管旁路校验
            TenantContext.runWithoutTenant(() -> {
                KnowledgeBaseEntity bypassA = knowledgeBaseDao.findById(kbAId);
                KnowledgeBaseEntity bypassB = knowledgeBaseDao.findById(kbBId);
                Assertions.assertNotNull(bypassA, "超管旁路应能查到A校知识库");
                Assertions.assertNotNull(bypassB, "超管旁路应能查到B校知识库");
            });

            // 6. Fail-Closed 校验
            TenantContext.clear();
            Assertions.assertNull(knowledgeBaseDao.findById(kbAId), "缺失租户上下文时必须触发 Fail-Closed，查询为空");
            Assertions.assertNull(knowledgeBaseDao.findById(kbBId), "缺失租户上下文时必须触发 Fail-Closed，查询为空");

        } finally {
            TenantContext.runWithoutTenant(() -> {
                knowledgeBaseMapper.deleteById(kbAId);
                knowledgeBaseMapper.deleteById(kbBId);
            });
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("3. [edu_question] 验证题库表物理行级隔离：A校/B校互不可见与超管旁路")
    void testQuestionRowLevelIsolation() {
        // 1. A 校 (tenantId = 1001) 插入题目
        TenantContext.setTenantId(1001L);
        QuestionEntity qA = new QuestionEntity();
        qA.setTenantId(1001L);
        qA.setStem("A校高等数学考题：求极限 lim(x->0) sinx/x");
        qA.setType("SINGLE_CHOICE");
        qA.setAnswer("1");
        qA.setDifficulty(3);
        qA.setStatus(1);
        qA.setDeleted(0);
        questionMapper.insert(qA);
        Long qAId = qA.getId();
        Assertions.assertNotNull(qAId);

        // 2. B 校 (tenantId = 1002) 插入题目
        TenantContext.setTenantId(1002L);
        QuestionEntity qB = new QuestionEntity();
        qB.setTenantId(1002L);
        qB.setStem("B校大学物理考题：麦克斯韦方程组的积分形式");
        qB.setType("SINGLE_CHOICE");
        qB.setAnswer("B");
        qB.setDifficulty(4);
        qB.setStatus(1);
        qB.setDeleted(0);
        questionMapper.insert(qB);
        Long qBId = qB.getId();
        Assertions.assertNotNull(qBId);

        try {
            // 3. A 校上下文校验
            TenantContext.setTenantId(1001L);
            QuestionEntity queryA = questionDao.getById(qAId);
            Assertions.assertNotNull(queryA, "A校必须能查询到自身题目");
            Assertions.assertEquals("A校高等数学考题：求极限 lim(x->0) sinx/x", queryA.getStem());

            QuestionEntity crossB = questionDao.getById(qBId);
            Assertions.assertNull(crossB, "A校绝不能越权查到B校题目");

            // 4. B 校上下文校验
            TenantContext.setTenantId(1002L);
            QuestionEntity queryB = questionDao.getById(qBId);
            Assertions.assertNotNull(queryB, "B校必须能查询到自身题目");

            QuestionEntity crossA = questionDao.getById(qAId);
            Assertions.assertNull(crossA, "B校绝不能越权查到A校题目");

            // 5. 超管旁路校验
            TenantContext.runWithoutTenant(() -> {
                QuestionEntity bypassA = questionDao.getById(qAId);
                QuestionEntity bypassB = questionDao.getById(qBId);
                Assertions.assertNotNull(bypassA, "超管旁路应能查到A校题目");
                Assertions.assertNotNull(bypassB, "超管旁路应能查到B校题目");
            });

            // 6. Fail-Closed 校验
            TenantContext.clear();
            Assertions.assertNull(questionDao.getById(qAId), "缺失租户上下文时必须触发 Fail-Closed，查询为空");
            Assertions.assertNull(questionDao.getById(qBId), "缺失租户上下文时必须触发 Fail-Closed，查询为空");

        } finally {
            TenantContext.runWithoutTenant(() -> {
                questionMapper.deleteById(qAId);
                questionMapper.deleteById(qBId);
            });
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("4. [ai_conversation] 验证AI会话表物理行级隔离：A校/B校互不可见与超管旁路")
    void testConversationRowLevelIsolation() {
        // 1. A 校 (tenantId = 1001) 插入会话
        TenantContext.setTenantId(1001L);
        ConversationEntity cA = new ConversationEntity();
        cA.setId(UUID.randomUUID().toString().replace("-", ""));
        cA.setTenantId(1001L);
        cA.setUserId(8881L);
        cA.setTitle("A校学员AI助教研讨会话");
        cA.setMessageCount(1);
        cA.setTotalTokens(50);
        cA.setDeleted(0);
        conversationMapper.insert(cA);
        String cAId = cA.getId();
        Assertions.assertNotNull(cAId);

        // 2. B 校 (tenantId = 1002) 插入会话
        TenantContext.setTenantId(1002L);
        ConversationEntity cB = new ConversationEntity();
        cB.setId(UUID.randomUUID().toString().replace("-", ""));
        cB.setTenantId(1002L);
        cB.setUserId(8882L);
        cB.setTitle("B校学员AI助教研讨会话");
        cB.setMessageCount(1);
        cB.setTotalTokens(60);
        cB.setDeleted(0);
        conversationMapper.insert(cB);
        String cBId = cB.getId();
        Assertions.assertNotNull(cBId);

        try {
            // 3. A 校上下文校验
            TenantContext.setTenantId(1001L);
            ConversationEntity queryA = conversationDao.findById(cAId);
            Assertions.assertNotNull(queryA, "A校必须能查询到自身会话");
            Assertions.assertEquals("A校学员AI助教研讨会话", queryA.getTitle());

            ConversationEntity crossB = conversationDao.findById(cBId);
            Assertions.assertNull(crossB, "A校绝不能越权查到B校会话");

            // 4. B 校上下文校验
            TenantContext.setTenantId(1002L);
            ConversationEntity queryB = conversationDao.findById(cBId);
            Assertions.assertNotNull(queryB, "B校必须能查询到自身会话");

            ConversationEntity crossA = conversationDao.findById(cAId);
            Assertions.assertNull(crossA, "B校绝不能越权查到A校会话");

            // 5. 超管旁路校验
            TenantContext.runWithoutTenant(() -> {
                ConversationEntity bypassA = conversationDao.findById(cAId);
                ConversationEntity bypassB = conversationDao.findById(cBId);
                Assertions.assertNotNull(bypassA, "超管旁路应能查到A校会话");
                Assertions.assertNotNull(bypassB, "超管旁路应能查到B校会话");
            });

            // 6. Fail-Closed 校验
            TenantContext.clear();
            Assertions.assertNull(conversationDao.findById(cAId), "缺失租户上下文时必须触发 Fail-Closed，查询为空");
            Assertions.assertNull(conversationDao.findById(cBId), "缺失租户上下文时必须触发 Fail-Closed，查询为空");

        } finally {
            TenantContext.runWithoutTenant(() -> {
                conversationMapper.deleteById(cAId);
                conversationMapper.deleteById(cBId);
            });
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("5. [ai_call_log] 验证AI调用日志表物理行级隔离：A校/B校互不可见与超管旁路")
    void testAiCallLogRowLevelIsolation() {
        final String sceneA = "ISO_AUDIT_A_" + System.currentTimeMillis();
        final String sceneB = "ISO_AUDIT_B_" + System.currentTimeMillis();

        // 1. A 校 (tenantId = 1001) 插入调用日志
        TenantContext.setTenantId(1001L);
        AiCallLogEntity logA = new AiCallLogEntity();
        logA.setTenantId(1001L);
        logA.setUserId(7771L);
        logA.setModel("deepseek-chat");
        logA.setScene(sceneA);
        logA.setPromptTokens(100);
        logA.setCompletionTokens(150);
        logA.setLatencyMs(350);
        aiCallLogDao.insert(logA);
        Long logAId = logA.getId();
        Assertions.assertNotNull(logAId);

        // 2. B 校 (tenantId = 1002) 插入调用日志
        TenantContext.setTenantId(1002L);
        AiCallLogEntity logB = new AiCallLogEntity();
        logB.setTenantId(1002L);
        logB.setUserId(7772L);
        logB.setModel("deepseek-chat");
        logB.setScene(sceneB);
        logB.setPromptTokens(80);
        logB.setCompletionTokens(120);
        logB.setLatencyMs(280);
        aiCallLogDao.insert(logB);
        Long logBId = logB.getId();
        Assertions.assertNotNull(logBId);

        try {
            // 3. A 校上下文校验
            TenantContext.setTenantId(1001L);
            AiCallLogEntity queryA = aiCallLogDao.findById(logAId);
            Assertions.assertNotNull(queryA, "A校必须能查询到自身AI调用日志");
            Assertions.assertEquals(sceneA, queryA.getScene());

            AiCallLogEntity crossB = aiCallLogDao.findById(logBId);
            Assertions.assertNull(crossB, "A校绝不能越权查到B校AI日志");

            long countSceneBInA = aiCallLogDao.count(new LambdaQueryWrapper<AiCallLogEntity>().eq(AiCallLogEntity::getScene, sceneB));
            Assertions.assertEquals(0, countSceneBInA, "A校通过条件聚合查B校场景日志必须为0");

            long countSceneAInA = aiCallLogDao.count(new LambdaQueryWrapper<AiCallLogEntity>().eq(AiCallLogEntity::getScene, sceneA));
            Assertions.assertEquals(1, countSceneAInA, "A校聚合自身场景日志应为1");

            // 4. B 校上下文校验
            TenantContext.setTenantId(1002L);
            AiCallLogEntity queryB = aiCallLogDao.findById(logBId);
            Assertions.assertNotNull(queryB, "B校必须能查询到自身AI调用日志");

            AiCallLogEntity crossA = aiCallLogDao.findById(logAId);
            Assertions.assertNull(crossA, "B校绝不能越权查到A校AI日志");

            long countSceneAInB = aiCallLogDao.count(new LambdaQueryWrapper<AiCallLogEntity>().eq(AiCallLogEntity::getScene, sceneA));
            Assertions.assertEquals(0, countSceneAInB, "B校通过条件聚合查A校场景日志必须为0");

            // 5. 超管旁路校验
            TenantContext.runWithoutTenant(() -> {
                AiCallLogEntity bypassA = aiCallLogDao.findById(logAId);
                AiCallLogEntity bypassB = aiCallLogDao.findById(logBId);
                Assertions.assertNotNull(bypassA, "超管旁路应能查到A校日志");
                Assertions.assertNotNull(bypassB, "超管旁路应能查到B校日志");
            });

            // 6. Fail-Closed 校验
            TenantContext.clear();
            Assertions.assertNull(aiCallLogDao.findById(logAId), "缺失租户上下文时必须触发 Fail-Closed，查询为空");
            Assertions.assertNull(aiCallLogDao.findById(logBId), "缺失租户上下文时必须触发 Fail-Closed，查询为空");

        } finally {
            TenantContext.runWithoutTenant(() -> {
                aiCallLogMapper.deleteById(logAId);
                aiCallLogMapper.deleteById(logBId);
            });
            TenantContext.clear();
        }
    }
}
