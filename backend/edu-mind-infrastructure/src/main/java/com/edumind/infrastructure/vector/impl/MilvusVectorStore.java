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
        R<io.milvus.grpc.MutationResult> response = client.insert(InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build());
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus insert failed: " + response.getMessage());
        }
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
                .withOutFields(List.of(ID_FIELD, KB_FIELD, DOC_FIELD))
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
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (filter.get("knowledgeBaseId") != null) {
            parts.add(KB_FIELD + " == " + filter.get("knowledgeBaseId"));
        }
        if (filter.get("documentId") != null) {
            parts.add(DOC_FIELD + " == " + filter.get("documentId"));
        }
        if (filter.get("tenantId") != null) {
            parts.add(TENANT_FIELD + " == " + filter.get("tenantId"));
        }
        return parts.isEmpty() ? null : String.join(" && ", parts);
    }

    private void ensureCollection(String collectionName) {
        MilvusServiceClient client = client();
        R<Boolean> exists = client.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
        if (Boolean.TRUE.equals(exists.getData())) {
            return;
        }
        List<FieldType> fields = List.of(
                FieldType.newBuilder().withName(ID_FIELD).withDataType(DataType.VarChar).withMaxLength(64).withPrimaryKey(true).build(),
                FieldType.newBuilder().withName(VECTOR_FIELD).withDataType(DataType.FloatVector).withDimension(milvusProperties.getDimension()).build(),
                FieldType.newBuilder().withName(KB_FIELD).withDataType(DataType.Int64).build(),
                FieldType.newBuilder().withName(DOC_FIELD).withDataType(DataType.Int64).build()
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
}
