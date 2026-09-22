package com.edumind.infrastructure.vector.impl;

import com.edumind.infrastructure.vector.VectorSearchResult;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CollectionSchemaParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "edumind.milvus.enabled", havingValue = "true")
public class MilvusVectorStore implements VectorStore {

    private static final String ID_FIELD = "chunk_id";
    private static final String VECTOR_FIELD = "embedding";
    private static final String KB_FIELD = "knowledge_base_id";
    private static final String DOC_FIELD = "document_id";
    private static final String TENANT_FIELD = "tenant_id";

    private final MilvusProperties milvusProperties;
    private final Map<String, MilvusServiceClient> clients = new ConcurrentHashMap<>();

    public MilvusVectorStore(MilvusProperties milvusProperties) {
        this.milvusProperties = milvusProperties;
    }

    @Override
    public void save(String collectionName, String id, List<Float> vector, Map<String, Object> metadata) {
        ensureCollection(collectionName);
        MilvusServiceClient client = client();
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(ID_FIELD, List.of(id)));
        fields.add(new InsertParam.Field(VECTOR_FIELD, List.of(vector)));
        fields.add(new InsertParam.Field(KB_FIELD, List.of(metadata.getOrDefault("knowledgeBaseId", 0L))));
        fields.add(new InsertParam.Field(DOC_FIELD, List.of(metadata.getOrDefault("documentId", 0L))));
        // 租户列必须真实落库，否则 searchNearest 的 tenant_id 过滤表达式将失效（跨租户向量召回）
        fields.add(new InsertParam.Field(TENANT_FIELD, List.of(resolveTenantId(metadata.get("tenantId")))));
        R<io.milvus.grpc.MutationResult> response = client.insert(InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build());
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus insert failed: " + response.getMessage());
        }
    }

    /**
     * 解析并强制校验向量归属租户：缺失租户时 Fail-Closed，绝不写入 0/默认租户
     * 避免无租户上下文的写入污染某个正式租户的召回结果。
     */
    private long resolveTenantId(Object raw) {
        if (raw instanceof Number num && num.longValue() > 0) {
            return num.longValue();
        }
        if (raw instanceof String str && !str.isBlank()) {
            try {
                long parsed = Long.parseLong(str.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // 落入下方 Fail-Closed 分支
            }
        }
        Long current = com.edumind.common.context.TenantContext.getTenantId();
        if (current != null && current > 0) {
            return current;
        }
        throw new IllegalStateException("向量写入缺少有效租户上下文，已拒绝写入 (Tenant Required)");
    }

    @Override
    public void delete(String collectionName, String id) {
        MilvusServiceClient client = client();
        client.delete(DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(ID_FIELD + " == \"" + id + "\"")
                .build());
    }

    @Override
    public List<String> searchNearestIds(String collectionName, List<Float> queryVector, int topK) {
        return searchNearest(collectionName, queryVector, topK).stream()
                .map(VectorSearchResult::getId)
                .toList();
    }

    @Override
    public List<VectorSearchResult> searchNearest(String collectionName, List<Float> queryVector, int topK,
                                                  Map<String, Object> filter) {
        ensureCollection(collectionName);
        String expr = buildFilterExpr(filter);
        MilvusServiceClient client = client();
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withTopK(topK)
                .withFloatVectors(List.of(queryVector))
                .withVectorFieldName(VECTOR_FIELD)
                .withOutFields(List.of(ID_FIELD, KB_FIELD, DOC_FIELD, TENANT_FIELD))
                .withExpr(expr)
                .build();
        R<SearchResults> response = client.search(searchParam);
        if (response.getStatus() != R.Status.Success.getCode() || response.getData() == null) {
            log.warn("Milvus search failed: {}", response.getMessage());
            return Collections.emptyList();
        }
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<VectorSearchResult> results = new ArrayList<>();
        if (wrapper.getRowRecords(0).isEmpty()) {
            return results;
        }
        for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
            Map<String, Object> row = wrapper.getRowRecords(0).get(i).getFieldValues();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("knowledgeBaseId", row.get(KB_FIELD));
            metadata.put("documentId", row.get(DOC_FIELD));
            metadata.put("tenantId", row.get(TENANT_FIELD));
            float score = wrapper.getIDScore(0).get(i).getScore();
            results.add(VectorSearchResult.builder()
                    .id(String.valueOf(row.get(ID_FIELD)))
                    .score(score)
                    .metadata(metadata)
                    .build());
        }
        return results;
    }

    private String buildFilterExpr(Map<String, Object> filter) {
        // Fail-Closed：缺乏租户维度的检索一律不返回任何数据，绝不退化为全量检索
        Object tenantId = filter == null ? null : filter.get("tenantId");
        if (tenantId == null) {
            tenantId = com.edumind.common.context.TenantContext.getTenantId();
        }
        if (tenantId == null) {
            log.warn("[向量检索] 缺少租户上下文，已按 Fail-Closed 返回 0 命中");
            return TENANT_FIELD + " < 0";
        }
        List<String> parts = new ArrayList<>();
        parts.add(TENANT_FIELD + " == " + tenantId);
        if (filter != null) {
            if (filter.get("knowledgeBaseId") != null) {
                parts.add(KB_FIELD + " == " + filter.get("knowledgeBaseId"));
            }
            if (filter.get("documentId") != null) {
                parts.add(DOC_FIELD + " == " + filter.get("documentId"));
            }
        }
        return String.join(" && ", parts);
    }

    private void ensureCollection(String collectionName) {
        MilvusServiceClient client = client();
        R<Boolean> exists = client.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
        if (Boolean.TRUE.equals(exists.getData())) {
            // 历史遗留 collection 可能缺少 tenant_id 字段，此时租户过滤表达式必然失效（跨租户召回）。
            // 向量可由 knowledge_chunk_index.embedding 重新生成，故安全起见重建 collection 并提示重新索引。
            if (!hasTenantField(client, collectionName)) {
                log.error("[严重] Milvus collection {} 缺少 {} 字段，租户隔离无法生效，正在重建（请重新触发知识库索引）",
                        collectionName, TENANT_FIELD);
                client.dropCollection(io.milvus.param.collection.DropCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build());
                createCollectionWithTenantField(client, collectionName);
            }
            return;
        }
        createCollectionWithTenantField(client, collectionName);
    }

    /**
     * 校验存量 collection 是否已包含租户列，避免旧结构导致 filter 静默失效
     */
    private boolean hasTenantField(MilvusServiceClient client, String collectionName) {
        try {
            var desc = client.describeCollection(io.milvus.param.collection.DescribeCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            if (desc == null || desc.getData() == null) {
                return false;
            }
            return desc.getData().getSchema().getFieldsList().stream()
                    .anyMatch(f -> TENANT_FIELD.equalsIgnoreCase(f.getName()));
        } catch (Exception e) {
            log.warn("校验 Milvus collection {} 结构失败，按缺失租户字段处理: {}", collectionName, e.getMessage());
            return false;
        }
    }

    private void createCollectionWithTenantField(MilvusServiceClient client, String collectionName) {
        List<FieldType> fields = List.of(
                FieldType.newBuilder().withName(ID_FIELD).withDataType(DataType.VarChar).withMaxLength(64).withPrimaryKey(true).build(),
                FieldType.newBuilder().withName(VECTOR_FIELD).withDataType(DataType.FloatVector).withDimension(milvusProperties.getDimension()).build(),
                FieldType.newBuilder().withName(KB_FIELD).withDataType(DataType.Int64).build(),
                FieldType.newBuilder().withName(DOC_FIELD).withDataType(DataType.Int64).build(),
                FieldType.newBuilder().withName(TENANT_FIELD).withDataType(DataType.Int64).build()
        );
        CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                .withFieldTypes(fields)
                .build();
        client.createCollection(CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withSchema(schema)
                .build());
        client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(VECTOR_FIELD)
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"nlist\":128}")
                .build());
    }

    private MilvusServiceClient client() {
        return clients.computeIfAbsent("default", key -> new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusProperties.getHost())
                        .withPort(milvusProperties.getPort())
                        .build()
        ));
    }

    @Override
    public String getEngineType() {
        return "Milvus";
    }

    @Override
    public String getVersion() {
        return "v2.3+";
    }

    @Override
    public boolean isHealthy() {
        try {
            MilvusServiceClient client = client();
            R<Boolean> res = client.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(milvusProperties.getCollection())
                    .build());
            return res != null && res.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.warn("Milvus health check failed: {}", e.getMessage());
            return false;
        }
    }
}
