package com.edumind.ai.integration.embedding;

import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.integration.crypto.AiApiKeyCipherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 与 {@link com.edumind.ai.integration.llm.LlmClientRegistry} 对齐：优先使用后台「模型配置」中
 * configType=embedding 的默认启用项，其次回退 application 中的 edumind.embedding。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClientRegistry {

    private final EmbeddingProperties embeddingProperties;
    private final AiModelConfigDao aiModelConfigDao;
    private final AiApiKeyCipherService aiApiKeyCipherService;

    private final AtomicReference<EmbeddingClient> cached = new AtomicReference<>();

    public synchronized EmbeddingClient getClient() {
        EmbeddingClient client = cached.get();
        if (client == null) {
            client = createClient();
            cached.set(client);
        }
        return client;
    }

    public void invalidate() {
        cached.set(null);
    }

    private EmbeddingClient createClient() {
        EmbeddingProperties props = resolveProperties();
        boolean hasKey = StringUtils.hasText(props.getApiKey());
        if (!hasKey) {
            if (embeddingProperties.isMockEnabled()) {
                log.warn("未配置 Embedding API Key（后台 embedding 模型与 edumind.embedding 均无 Key），使用 Mock 向量");
                return new MockEmbeddingClient(props);
            }
            log.warn("未配置 Embedding API Key，仍使用 Mock 向量（避免检索链路中断）");
            return new MockEmbeddingClient(props);
        }
        log.info("Embedding 使用真实 API：model={} baseUrl={}", props.getModel(), props.getBaseUrl());
        return new OpenAiCompatibleEmbeddingClient(props);
    }

    private EmbeddingProperties resolveProperties() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setBaseUrl(embeddingProperties.getBaseUrl());
        props.setApiKey(embeddingProperties.getApiKey());
        props.setModel(embeddingProperties.getModel());
        props.setDimensions(embeddingProperties.getDimensions());
        props.setBatchSize(embeddingProperties.getBatchSize());
        props.setMockEnabled(embeddingProperties.isMockEnabled());

        AiModelConfigEntity config = aiModelConfigDao.findDefaultByType("embedding");
        if (config != null) {
            if (StringUtils.hasText(config.getModelName())) {
                props.setModel(config.getModelName());
            }
            if (StringUtils.hasText(config.getBaseUrl())) {
                props.setBaseUrl(config.getBaseUrl());
            }
            String dbKey = aiApiKeyCipherService.decrypt(config.getApiKeyCipher(), config.getKeyVersion());
            if (StringUtils.hasText(dbKey)) {
                props.setApiKey(dbKey);
            }
            if (config.getDimension() != null && config.getDimension() > 0) {
                props.setDimensions(config.getDimension());
            }
        }
        return props;
    }
}
