package com.edumind;

import com.edumind.common.context.TenantContext;
import com.edumind.infrastructure.vector.VectorSearchResult;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.impl.InMemoryVectorStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * 向量检索租户隔离回归测试（不依赖 Milvus / Spring 容器，纯内存实现即可验证隔离契约）
 *
 * <p>覆盖三类历史缺陷：</p>
 * <ol>
 *   <li>缺少租户归属的向量被写入，污染其它租户召回结果；</li>
 *   <li>检索时未传租户过滤条件，退化为全量检索造成跨租户召回；</li>
 *   <li>VectorStore 默认方法忽略 filter 直接全量检索，形成静默泄漏通道。</li>
 * </ol>
 *
 * <p>说明：Milvus 实现（{@code edumind.milvus.enabled=true}）的等价隔离语义由
 * {@code MilvusVectorStore} 的 tenant_id 字段建表 + 表达式过滤保证，
 * 并已通过 {@code ensureCollection} 中的结构校验（缺失 tenant_id 时重建 collection）兜底，
 * 无法在无 Milvus 服务的单测环境中执行，故此处校验契约层与内存实现。</p>
 */
public class VectorTenantIsolationTest {

    private static final String COLLECTION = "edumind_chunk_vector";
    private static final Long TENANT_A = 1001L;
    private static final Long TENANT_B = 1002L;

    private final InMemoryVectorStore vectorStore = new InMemoryVectorStore();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("向量写入必须携带租户归属，缺失租户时 Fail-Closed 拒绝写入")
    void saveWithoutTenantMustFailClosed() {
        TenantContext.clear();
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> vectorStore.save(COLLECTION, "chunk-1", List.of(0.1f, 0.2f), Map.of("knowledgeBaseId", 9L)),
                "无租户上下文写入向量必须被拒绝，避免污染某个正式租户的召回结果");
        Assertions.assertTrue(ex.getMessage().contains("Tenant Required"));
    }

    @Test
    @DisplayName("写入时可从上下文自动补齐租户，且检索不得跨租户召回")
    void crossTenantRetrievalMustNotLeak() {
        List<Float> vector = List.of(1.0f, 0.0f);

        // A 校写入
        TenantContext.setTenantId(TENANT_A);
        vectorStore.save(COLLECTION, "chunk-A", vector, Map.of("knowledgeBaseId", 7L));

        // B 校写入
        TenantContext.setTenantId(TENANT_B);
        vectorStore.save(COLLECTION, "chunk-B", vector, Map.of("knowledgeBaseId", 7L));

        // A 校检索：只能召回本校向量
        TenantContext.setTenantId(TENANT_A);
        List<VectorSearchResult> resultsA = vectorStore.searchNearest(COLLECTION, vector, 10,
                Map.of("knowledgeBaseId", 7L, "tenantId", TENANT_A));
        Assertions.assertEquals(1, resultsA.size(), "A 校检索必须只命中本校 1 条向量");
        Assertions.assertEquals("chunk-A", resultsA.get(0).getId());
        Assertions.assertTrue(resultsA.stream().noneMatch(r -> "chunk-B".equals(r.getId())),
                "A 校检索绝不可召回 B 校向量");

        // B 校检索：只能召回本校向量
        TenantContext.setTenantId(TENANT_B);
        List<VectorSearchResult> resultsB = vectorStore.searchNearest(COLLECTION, vector, 10,
                Map.of("knowledgeBaseId", 7L, "tenantId", TENANT_B));
        Assertions.assertEquals(1, resultsB.size(), "B 校检索必须只命中本校 1 条向量");
        Assertions.assertEquals("chunk-B", resultsB.get(0).getId());
    }

    @Test
    @DisplayName("检索未指定租户且上下文为空时按 Fail-Closed 返回 0 命中，绝不退化为全量检索")
    void searchWithoutTenantMustReturnNothing() {
        // 先在有租户上下文时写入数据
        TenantContext.setTenantId(TENANT_A);
        vectorStore.save(COLLECTION, "chunk-A", List.of(1.0f, 0.0f), Map.of("knowledgeBaseId", 7L));

        // 清理上下文后检索：必须 0 命中
        TenantContext.clear();
        List<VectorSearchResult> results = vectorStore.searchNearest(COLLECTION, List.of(1.0f, 0.0f), 10,
                Map.of("knowledgeBaseId", 7L));
        Assertions.assertTrue(results.isEmpty(),
                "无租户维度的检索必须 0 命中，避免跨租户全量召回");
    }

    @Test
    @DisplayName("VectorStore 默认实现必须拒绝带过滤条件的检索，杜绝静默忽略 filter 的泄漏通道")
    void defaultFilteredSearchMustRefuse() {
        VectorStore minimal = new VectorStore() {
            @Override
            public void save(String collectionName, String id, List<Float> vector, Map<String, Object> metadata) {
                // 空实现：仅用于验证接口契约
            }

            @Override
            public void delete(String collectionName, String id) {
                // 空实现：仅用于验证接口契约
            }

            @Override
            public List<String> searchNearestIds(String collectionName, List<Float> queryVector, int topK) {
                return List.of("chunk-A");
            }
        };

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> minimal.searchNearest(COLLECTION, List.of(1.0f), 5, Map.of("tenantId", TENANT_A)),
                "未实现租户过滤的向量存储必须显式抛错，而不是静默返回全量结果");
    }
}
