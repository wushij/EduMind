package com.edumind;

import com.edumind.common.context.TenantContext;
import com.edumind.common.utils.TenantObjectKeyBuilder;
import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.infrastructure.redis.cache.AiQuotaCache;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import com.edumind.infrastructure.redis.cache.DashboardCacheService;
import com.edumind.infrastructure.vector.VectorSearchResult;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.config.MilvusProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 智教云 V2.0 · Gate I2 全介质租户隔离集成测试
 * 覆盖 Redis / OSS / Vector 全介质多租户隔离与越权防御验证
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TenantMediumIsolationIntegrationTest {

    @Autowired
    private AiSessionCacheService aiSessionCacheService;

    @Autowired
    private DashboardCacheService dashboardCacheService;

    @Autowired
    private AiQuotaCache aiQuotaCache;

    @Autowired
    private RedisService redisService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private MilvusProperties milvusProperties;

    private static final Long TENANT_A = 1001L;
    private static final Long TENANT_B = 1002L;

    @BeforeEach
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Redis 会话缓存租户隔离：A校写入会话，B校读取为空，各自隔离")
    void testRedisSessionIsolation() {
        String conversationId = "conv-iso-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            // 1. A 校上下文写入会话
            TenantContext.setTenantId(TENANT_A);
            AiSessionCacheService.AiSessionState stateA = new AiSessionCacheService.AiSessionState();
            stateA.setConversationId(conversationId);
            stateA.setUserId(8001L);
            stateA.setCourseId(101L);
            stateA.setStreamingContent("Tenant A private chat");
            aiSessionCacheService.saveSession(stateA);

            // A 校可正常读取
            AiSessionCacheService.AiSessionState readA = aiSessionCacheService.getSession(conversationId);
            Assertions.assertNotNull(readA, "A 校应可读取自身会话");
            Assertions.assertEquals("Tenant A private chat", readA.getStreamingContent());

            // 2. 切换至 B 校上下文读取同一个 conversationId
            TenantContext.setTenantId(TENANT_B);
            AiSessionCacheService.AiSessionState readB = aiSessionCacheService.getSession(conversationId);
            Assertions.assertNull(readB, "B 校跨租户读取 A 校会话必须返回 null");

            // B 校写入自己的会话状态
            AiSessionCacheService.AiSessionState stateB = new AiSessionCacheService.AiSessionState();
            stateB.setConversationId(conversationId);
            stateB.setUserId(9001L);
            stateB.setCourseId(202L);
            stateB.setStreamingContent("Tenant B private chat");
            aiSessionCacheService.saveSession(stateB);

            // B 校读取自己的会话
            AiSessionCacheService.AiSessionState readB2 = aiSessionCacheService.getSession(conversationId);
            Assertions.assertNotNull(readB2);
            Assertions.assertEquals("Tenant B private chat", readB2.getStreamingContent());

            // 3. 切回 A 校，验证 A 校会话未被 B 校覆盖污染
            TenantContext.setTenantId(TENANT_A);
            AiSessionCacheService.AiSessionState readA2 = aiSessionCacheService.getSession(conversationId);
            Assertions.assertNotNull(readA2);
            Assertions.assertEquals("Tenant A private chat", readA2.getStreamingContent(), "A 校缓存数据不应被 B 校覆盖");
        } finally {
            TenantContext.setTenantId(TENANT_A);
            aiSessionCacheService.deleteSession(conversationId);
            TenantContext.setTenantId(TENANT_B);
            aiSessionCacheService.deleteSession(conversationId);
        }
    }

    @Test
    @DisplayName("Redis 看板与配额租户隔离：相同 userId 在不同租户的缓存与配额相互独立")
    void testRedisDashboardAndQuotaIsolation() {
        Long sharedUserId = 70000L + (long) (Math.random() * 50000L);
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        try {
            // 1. A 校加载看板缓存
            TenantContext.setTenantId(TENANT_A);
            String valA = dashboardCacheService.getOrLoad(sharedUserId, String.class, () -> "DASHBOARD_DATA_TENANT_A");
            Assertions.assertEquals("DASHBOARD_DATA_TENANT_A", valA);

            // 2. B 校查询相同 userId，触发自己的 loader
            TenantContext.setTenantId(TENANT_B);
            String valB = dashboardCacheService.getOrLoad(sharedUserId, String.class, () -> "DASHBOARD_DATA_TENANT_B");
            Assertions.assertEquals("DASHBOARD_DATA_TENANT_B", valB, "B 校必须返回自己的看板数据，不可命中 A 校缓存");

            // 切回 A 校读取，仍为 A 校缓存
            TenantContext.setTenantId(TENANT_A);
            String valA2 = dashboardCacheService.getOrLoad(sharedUserId, String.class, () -> "SHOULD_NOT_LOAD");
            Assertions.assertEquals("DASHBOARD_DATA_TENANT_A", valA2);

            // 3. 配额隔离：A 校消费配额，不影响 B 校
            TenantContext.setTenantId(TENANT_A);
            boolean allowedA1 = aiQuotaCache.checkAndIncrementDailyQuota(sharedUserId, 1);
            boolean allowedA2 = aiQuotaCache.checkAndIncrementDailyQuota(sharedUserId, 1);
            Assertions.assertTrue(allowedA1, "A 校首次配额允许");
            Assertions.assertFalse(allowedA2, "A 校超出配额拒绝");

            // B 校此时配额仍有额度
            TenantContext.setTenantId(TENANT_B);
            boolean allowedB1 = aiQuotaCache.checkAndIncrementDailyQuota(sharedUserId, 1);
            Assertions.assertTrue(allowedB1, "B 校相同用户的独立配额不受 A 校耗尽影响");
        } finally {
            TenantContext.setTenantId(TENANT_A);
            dashboardCacheService.evict(sharedUserId);
            redisService.delete(com.edumind.infrastructure.redis.RedisKeyBuilder.aiQuota(sharedUserId, today));
            TenantContext.setTenantId(TENANT_B);
            dashboardCacheService.evict(sharedUserId);
            redisService.delete(com.edumind.infrastructure.redis.RedisKeyBuilder.aiQuota(sharedUserId, today));
        }
    }

    @Test
    @DisplayName("Vector 向量检索租户隔离：两校分别入库向量，精准匹配本校 tenantId，交叉检索 0 命中")
    void testVectorMetadataIsolation() {
        String collection = milvusProperties.getCollection();
        String chunkA = "vector-chunk-1001-" + UUID.randomUUID().toString().substring(0, 6);
        String chunkB = "vector-chunk-1002-" + UUID.randomUUID().toString().substring(0, 6);
        Long sharedKbId = 7788L;

        List<Float> vectorData = List.of(0.12f, 0.34f, 0.56f, 0.78f);

        // 1. A 校写入向量，metadata 注入 tenantId = 1001
        Map<String, Object> metaA = Map.of(
                "chunkId", 10001L,
                "knowledgeBaseId", sharedKbId,
                "tenantId", TENANT_A
        );
        vectorStore.save(collection, chunkA, vectorData, metaA);

        // 2. B 校写入向量，metadata 注入 tenantId = 1002
        Map<String, Object> metaB = Map.of(
                "chunkId", 10002L,
                "knowledgeBaseId", sharedKbId,
                "tenantId", TENANT_B
        );
        vectorStore.save(collection, chunkB, vectorData, metaB);

        // 3. A 校检索带 tenantId = 1001 过滤条件
        Map<String, Object> filterA = Map.of(
                "knowledgeBaseId", sharedKbId,
                "tenantId", TENANT_A
        );
        List<VectorSearchResult> resultsA = vectorStore.searchNearest(collection, vectorData, 10, filterA);
        Assertions.assertFalse(resultsA.isEmpty(), "A 校检索必须命中本校向量");
        Assertions.assertTrue(resultsA.stream().anyMatch(r -> chunkA.equals(r.getId())));
        Assertions.assertTrue(resultsA.stream().noneMatch(r -> chunkB.equals(r.getId())), "A 校检索绝不可召回 B 校向量");

        // 4. B 校检索带 tenantId = 1002 过滤条件
        Map<String, Object> filterB = Map.of(
                "knowledgeBaseId", sharedKbId,
                "tenantId", TENANT_B
        );
        List<VectorSearchResult> resultsB = vectorStore.searchNearest(collection, vectorData, 10, filterB);
        Assertions.assertFalse(resultsB.isEmpty(), "B 校检索必须命中本校向量");
        Assertions.assertTrue(resultsB.stream().anyMatch(r -> chunkB.equals(r.getId())));
        Assertions.assertTrue(resultsB.stream().noneMatch(r -> chunkA.equals(r.getId())), "B 校检索绝不可召回 A 校向量");

        // 5. 第三方未授权租户检索相同知识库 ID
        Map<String, Object> filterC = Map.of(
                "knowledgeBaseId", sharedKbId,
                "tenantId", 9999L
        );
        List<VectorSearchResult> resultsC = vectorStore.searchNearest(collection, vectorData, 10, filterC);
        Assertions.assertTrue(resultsC.isEmpty(), "未授权租户交叉检索必须 0 命中");
    }

    @Test
    @DisplayName("文件存储租户路径与越权隔离：ObjectKey 含 tenants/{id} 前缀，跨租户读写阻断")
    void testObjectKeyTenantPrefixAndIsolation() throws Exception {
        // 1. 验证 ObjectKey 规范前缀生成
        String docKey = TenantObjectKeyBuilder.knowledgeDocument(TENANT_A, 55L, "uuid-test", "syllabus.pdf");
        Assertions.assertEquals("tenants/1001/knowledge/55/uuid-test/syllabus.pdf", docKey);

        String avatarKey = TenantObjectKeyBuilder.userAvatar(TENANT_A, 123L, "avatar.png");
        Assertions.assertEquals("tenants/1001/users/123/avatar/avatar.png", avatarKey);

        // 2. 验证租户所属权静态判定
        Assertions.assertTrue(TenantObjectKeyBuilder.validateTenantOwnership(TENANT_A, docKey));
        Assertions.assertFalse(TenantObjectKeyBuilder.validateTenantOwnership(TENANT_B, docKey), "B 校访问 A 校 ObjectKey 必须校验失败");
        Assertions.assertTrue(TenantObjectKeyBuilder.validateTenantOwnership(null, docKey), "平台超管/旁路必须校验通过");

        // 兼容历史存量无 tenants/ 前缀文件
        Assertions.assertTrue(TenantObjectKeyBuilder.validateTenantOwnership(TENANT_B, "knowledge/55/legacy.pdf"));

        // 3. 验证本地磁盘/对象存储服务级跨租户读隔离
        String testContent = "Secret tenant 1001 course material";
        fileStorageService.uploadFile("edumind", docKey,
                new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8)), "text/plain");

        try {
            // A 校读取：成功返回文件流
            TenantContext.setTenantId(TENANT_A);
            try (InputStream inA = fileStorageService.getFile("edumind", docKey)) {
                Assertions.assertNotNull(inA, "A 校读取自身文件必须成功");
                String content = new String(inA.readAllBytes(), StandardCharsets.UTF_8);
                Assertions.assertEquals(testContent, content);
            }

            // B 校读取：返回 null，被安全拦截
            TenantContext.setTenantId(TENANT_B);
            try (InputStream inB = fileStorageService.getFile("edumind", docKey)) {
                Assertions.assertNull(inB, "B 校跨校读取 A 校 ObjectKey 必须被阻断并返回 null");
            }
        } finally {
            // A 校安全清理
            TenantContext.setTenantId(TENANT_A);
            fileStorageService.deleteFile("edumind", docKey);
        }
    }
}
