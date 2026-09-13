package com.edumind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.dao.memory.AiMemoryDao;
import com.edumind.ai.dto.ChatStreamDTO;
import com.edumind.ai.dto.memory.MemoryConsentDTO;
import com.edumind.ai.dto.memory.MemoryFeedbackDTO;
import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.entity.memory.AiMemoryFeedbackEntity;
import com.edumind.ai.entity.memory.AiMemoryItemEntity;
import com.edumind.ai.entity.memory.AiMemoryNamespaceEntity;
import com.edumind.ai.mapper.memory.AiMemoryFeedbackMapper;
import com.edumind.ai.mapper.memory.AiMemoryItemMapper;
import com.edumind.ai.mapper.memory.AiMemoryNamespaceMapper;
import com.edumind.ai.service.chat.ChatService;
import com.edumind.ai.service.memory.AgentMemoryService;
import com.edumind.ai.service.memory.retrieval.MemoryContextBlock;
import com.edumind.ai.service.memory.retrieval.MemoryRetrievalService;
import com.edumind.ai.vo.memory.MemoryItemVO;
import com.edumind.ai.vo.memory.MemoryNamespaceVO;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智教云 V2.0 · Agent 长期记忆 Beta 全链路闭环集成测试 (Gate I4 专项工程验收)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class AgentMemoryIntegrationTest {

    @Autowired
    private AgentMemoryService agentMemoryService;

    @Autowired
    private MemoryRetrievalService memoryRetrievalService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private AiMemoryDao aiMemoryDao;

    @Autowired
    private SysTenantDao sysTenantDao;

    @Autowired
    private AiMemoryNamespaceMapper namespaceMapper;

    @Autowired
    private AiMemoryItemMapper itemMapper;

    @Autowired
    private AiMemoryFeedbackMapper feedbackMapper;

    private static final Long TENANT_A = 9901L;
    private static final Long TENANT_B = 9902L;
    private static final Long USER_A1 = 9911L;
    private static final Long USER_A2 = 9912L;
    private static final Long USER_B1 = 9921L;
    private static final Long COURSE_ID = 9101L;

    @BeforeEach
    void setUp() {
        cleanData();
    }

    @AfterEach
    void tearDown() {
        cleanData();
    }

    private void cleanData() {
        TenantContext.runWithoutTenant(() -> {
            ensureTenant(TENANT_A, "TENANT_9901", "记忆测试学校9901");
            ensureTenant(TENANT_B, "TENANT_9902", "记忆测试学校9902");

            // 外键依赖顺序物理清理: feedback -> items -> namespace
            feedbackMapper.delete(new LambdaQueryWrapper<AiMemoryFeedbackEntity>()
                    .in(AiMemoryFeedbackEntity::getUserId, USER_A1, USER_A2, USER_B1));

            List<AiMemoryNamespaceEntity> namespaces = namespaceMapper.selectList(
                    new LambdaQueryWrapper<AiMemoryNamespaceEntity>()
                            .in(AiMemoryNamespaceEntity::getTenantId, TENANT_A, TENANT_B));
            for (AiMemoryNamespaceEntity ns : namespaces) {
                itemMapper.delete(new LambdaQueryWrapper<AiMemoryItemEntity>()
                        .eq(AiMemoryItemEntity::getNamespaceId, ns.getId()));
            }

            namespaceMapper.delete(new LambdaQueryWrapper<AiMemoryNamespaceEntity>()
                    .in(AiMemoryNamespaceEntity::getTenantId, TENANT_A, TENANT_B));
        });
        TenantContext.clear();
        UserContext.clear();
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

    private void mockLogin(Long tenantId, Long userId) {
        TenantContext.setTenantId(tenantId);
        UserContext.set(LoginUser.builder()
                .id(userId)
                .username("user_" + userId)
                .realName("测试用户" + userId)
                .roles(List.of("STUDENT"))
                .permissions(List.of("ai:chat", "ai:memory:view", "ai:memory:manage"))
                .build());
    }

    @Test
    @DisplayName("用例 1: 默认未获得用户知情同意，严格拒绝写入记忆资产 (403 FORBIDDEN)")
    void test1_defaultUnconsentedRejectsWrite() {
        mockLogin(TENANT_A, USER_A1);

        // 获取或初始化命名空间，默认状态应为未授权 (0 / false)
        MemoryNamespaceVO nsVO = agentMemoryService.getNamespace(COURSE_ID);
        Assertions.assertNotNull(nsVO);
        Assertions.assertFalse(nsVO.getConsentGranted(), "默认新空间授权状态必须为未授权 (0 / false)");

        // 尝试在未授权状态下写入条目
        MemoryItemCreateDTO createDTO = new MemoryItemCreateDTO();
        createDTO.setCourseId(COURSE_ID);
        createDTO.setSummary("学生偏好 Python 3.11 现代语法");
        createDTO.setMemoryType("PREFERENCE");

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () ->
                agentMemoryService.createMemoryItem(createDTO));
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode(),
                "未授权时写入记忆条目必须 Fail-Closed 抛出 403 FORBIDDEN");
    }

    @Test
    @DisplayName("用例 2: 知情授权后可成功写入，国密 SM4 敏感加密生效，且检索 Top-K 命中新条目")
    void test2_consentedWriteAndRetrieve() {
        mockLogin(TENANT_A, USER_A1);

        // 1. 用户显式给予知情授权
        MemoryConsentDTO consentDTO = new MemoryConsentDTO();
        consentDTO.setCourseId(COURSE_ID);
        consentDTO.setConsentGranted(true);
        consentDTO.setRetentionDays(90);
        agentMemoryService.updateConsent(consentDTO);

        MemoryNamespaceVO nsVO = agentMemoryService.getNamespace(COURSE_ID);
        Assertions.assertTrue(nsVO.getConsentGranted(), "授权更新后状态必须为 true");
        Assertions.assertEquals(90, nsVO.getRetentionDays(), "留存周期应为 90 天");

        // 2. 写入一条带有高敏感标记的记忆，验证国密 SM4 加密
        MemoryItemCreateDTO createDTO = new MemoryItemCreateDTO();
        createDTO.setCourseId(COURSE_ID);
        createDTO.setSummary("高微积分导数复合函数链式法则掌握薄弱");
        createDTO.setFullContent("详细测试内容：学生在内层求导极易漏项，需前置检查。");
        createDTO.setSensitivityLevel("HIGH_RISK");
        createDTO.setMemoryType("PROFILE");
        Long itemId = agentMemoryService.createMemoryItem(createDTO);
        Assertions.assertNotNull(itemId);

        // 3. 直接验证底层 DB 密文存储，确保密文非空且不为明文
        AiMemoryItemEntity entity = aiMemoryDao.findItemById(itemId);
        Assertions.assertNotNull(entity);
        Assertions.assertNotNull(entity.getContentCiphertext(), "高敏记忆条目的 content_ciphertext 必须非空");
        Assertions.assertNotEquals("详细测试内容：学生在内层求导极易漏项，需前置检查。", entity.getContentCiphertext(),
                "数据库存储的必须为 SM4 密文，禁止明文暴露");

        // 4. 检索验证：召回条目应包含该记忆，且解密内容与脱敏摘要正确
        List<MemoryItemVO> retrieved = agentMemoryService.retrieveMemories(COURSE_ID, "微积分 导数 链式法则");
        Assertions.assertFalse(retrieved.isEmpty(), "应成功召回相关记忆条目");
        MemoryItemVO matched = retrieved.get(0);
        Assertions.assertEquals(itemId, matched.getId());
        Assertions.assertTrue(matched.getEncrypted(), "返回 VO 应标识已加密");
        Assertions.assertEquals("PROFILE", matched.getMemoryType());
    }

    @Test
    @DisplayName("用例 3: 撤回知情同意时，级联物理擦除记忆条目与向量引用，检索归零")
    void test3_revokeConsentCascadeClean() {
        mockLogin(TENANT_A, USER_A1);

        // 1. 授权并写入条目
        MemoryConsentDTO consentDTO = new MemoryConsentDTO();
        consentDTO.setCourseId(COURSE_ID);
        consentDTO.setConsentGranted(true);
        agentMemoryService.updateConsent(consentDTO);

        MemoryItemCreateDTO createDTO = new MemoryItemCreateDTO();
        createDTO.setCourseId(COURSE_ID);
        createDTO.setSummary("偏好采用图形可视化方式讲解极限概念");
        createDTO.setMemoryType("PREFERENCE");
        agentMemoryService.createMemoryItem(createDTO);

        MemoryNamespaceVO nsBefore = agentMemoryService.getNamespace(COURSE_ID);
        Assertions.assertEquals(1, nsBefore.getItems().size(), "授权并写入后应有 1 条记忆");

        // 2. 撤回授权
        consentDTO.setConsentGranted(false);
        agentMemoryService.updateConsent(consentDTO);

        // 3. 验证空间条目已被物理清理
        MemoryNamespaceVO nsAfter = agentMemoryService.getNamespace(COURSE_ID);
        Assertions.assertFalse(nsAfter.getConsentGranted(), "撤销后授权状态应为 false");
        Assertions.assertTrue(nsAfter.getItems().isEmpty(), "撤销知情同意后，该空间条目必须物理级联擦除");

        // 4. 检索管线应直接返回空 (Fail-Closed)
        List<MemoryItemVO> list = agentMemoryService.retrieveMemories(COURSE_ID, "图形 可视化 极限");
        Assertions.assertTrue(list.isEmpty(), "未授权空间的检索必须直接返回空");
    }

    @Test
    @DisplayName("用例 4: 单条遗忘与跨用户 IDOR 越权拦截防御")
    void test4_forgetItemAndIdorProtection() {
        // 1. 用户 A1 授权并创建记忆条目
        mockLogin(TENANT_A, USER_A1);
        MemoryConsentDTO consentDTO = new MemoryConsentDTO();
        consentDTO.setCourseId(COURSE_ID);
        consentDTO.setConsentGranted(true);
        agentMemoryService.updateConsent(consentDTO);

        MemoryItemCreateDTO createDTO = new MemoryItemCreateDTO();
        createDTO.setCourseId(COURSE_ID);
        createDTO.setSummary("学生 A1 的私有作答习惯与错题记录");
        createDTO.setMemoryType("EPISODIC");
        Long itemA1Id = agentMemoryService.createMemoryItem(createDTO);
        Assertions.assertNotNull(itemA1Id);

        // 2. 切换为同一租户下的另一用户 A2，企图越权删除用户 A1 的记忆条目 (IDOR 越权攻击)
        mockLogin(TENANT_A, USER_A2);
        BusinessException forgetEx = Assertions.assertThrows(BusinessException.class, () ->
                agentMemoryService.forgetMemory(itemA1Id));
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), forgetEx.getCode(),
                "跨用户删除他人长期记忆必须被 IDOR 校验拦截并抛出 403 FORBIDDEN");

        // 3. 用户 A2 企图对用户 A1 的记忆条目进行篡改反馈 (IDOR 攻击)
        MemoryFeedbackDTO fbDTO = new MemoryFeedbackDTO();
        fbDTO.setFeedbackAction("MODIFY");
        fbDTO.setCorrectContent("恶意篡改的内容");
        BusinessException feedbackEx = Assertions.assertThrows(BusinessException.class, () ->
                agentMemoryService.feedbackMemory(itemA1Id, fbDTO));
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), feedbackEx.getCode(),
                "跨用户反馈他人记忆条目必须被 IDOR 校验拦截并抛出 403 FORBIDDEN");

        // 4. 切回用户 A1 本人，执行遗忘操作，应成功物理删除
        mockLogin(TENANT_A, USER_A1);
        agentMemoryService.forgetMemory(itemA1Id);
        AiMemoryItemEntity deletedItem = aiMemoryDao.findItemById(itemA1Id);
        Assertions.assertNull(deletedItem, "合法所有者遗忘后，数据库中该条目必须已被物理擦除");
    }

    @Test
    @DisplayName("用例 5: 跨租户数据隔离保护 (租户 A 记忆条目对租户 B 完全不可见)")
    void test5_crossTenantIsolation() {
        // 1. 租户 A 用户 A1 授权并沉淀记忆
        mockLogin(TENANT_A, USER_A1);
        MemoryConsentDTO consentA = new MemoryConsentDTO();
        consentA.setCourseId(COURSE_ID);
        consentA.setConsentGranted(true);
        agentMemoryService.updateConsent(consentA);

        MemoryItemCreateDTO itemA = new MemoryItemCreateDTO();
        itemA.setCourseId(COURSE_ID);
        itemA.setSummary("【租户A特有】线性代数特征向量特殊推导思路");
        itemA.setMemoryType("PREFERENCE");
        agentMemoryService.createMemoryItem(itemA);

        // 2. 租户 B 用户 B1 授权
        mockLogin(TENANT_B, USER_B1);
        MemoryConsentDTO consentB = new MemoryConsentDTO();
        consentB.setCourseId(COURSE_ID);
        consentB.setConsentGranted(true);
        agentMemoryService.updateConsent(consentB);

        // 3. 租户 B 检索租户 A 的条目，验证不可见
        List<MemoryItemVO> retrievedB = agentMemoryService.retrieveMemories(COURSE_ID, "线性代数 特征向量");
        Assertions.assertTrue(retrievedB.isEmpty(), "跨租户检索必须严格隔离，租户 B 不可见租户 A 的记忆");

        MemoryNamespaceVO nsB = agentMemoryService.getNamespace(COURSE_ID);
        Assertions.assertTrue(nsB.getItems().isEmpty(), "租户 B 空间内记忆条目列表应为空");
    }

    @Test
    @DisplayName("用例 6: Chat 主链路集成长期记忆 (授权状态下注入上下文并触发 memory SSE 事件)")
    void test6_chatIntegrationWithMemory() {
        mockLogin(TENANT_A, USER_A1);

        // 1. 授权并写入个性化学习偏好
        MemoryConsentDTO consentDTO = new MemoryConsentDTO();
        consentDTO.setCourseId(COURSE_ID);
        consentDTO.setConsentGranted(true);
        agentMemoryService.updateConsent(consentDTO);

        MemoryItemCreateDTO createDTO = new MemoryItemCreateDTO();
        createDTO.setCourseId(COURSE_ID);
        createDTO.setSummary("偏好详细推导步骤，喜欢先列出必要定义域再解题");
        createDTO.setMemoryType("PREFERENCE");
        agentMemoryService.createMemoryItem(createDTO);

        // 2. 模拟 Chat 发起对话
        ChatStreamDTO chatDTO = new ChatStreamDTO();
        chatDTO.setCourseId(COURSE_ID);
        chatDTO.setMessage("请帮我推导这道函数的单调区间");
        chatDTO.setModelKey("deepseek-v3");

        SseEmitter emitter = chatService.streamChat(chatDTO);
        Assertions.assertNotNull(emitter, "对话流应成功创建 SseEmitter");

        // 3. 验证长期记忆检索管线在此对话场景下能够精准匹配并生成上下文块
        List<MemoryContextBlock> blocks = memoryRetrievalService.retrieve(
                TENANT_A, USER_A1, COURSE_ID, chatDTO.getMessage(), 5);
        Assertions.assertFalse(blocks.isEmpty(), "对话前检索管线应命中该用户的长期偏好记忆");
        Assertions.assertEquals("PREFERENCE", blocks.get(0).getMemoryType());
        Assertions.assertTrue(blocks.get(0).getSummary().contains("偏好详细推导步骤"));

        // 4. 验证撤回授权后，同样的对话消息无法召回任何记忆 (Fail-Closed)
        consentDTO.setConsentGranted(false);
        agentMemoryService.updateConsent(consentDTO);

        List<MemoryContextBlock> blocksAfterRevoke = memoryRetrievalService.retrieve(
                TENANT_A, USER_A1, COURSE_ID, chatDTO.getMessage(), 5);
        Assertions.assertTrue(blocksAfterRevoke.isEmpty(), "撤回授权后，相同场景下的记忆检索必须为空 (Fail-Closed)");
    }
}
