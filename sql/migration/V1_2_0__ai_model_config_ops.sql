-- V1.2.0 AI 模型运维字段（对齐 goblog-server blog_ai_model_config）
-- 支持：config_name 业务键、chat/embedding 双类型、API Key 密文、思考强度、向量维度、默认模型

ALTER TABLE ai_model_config
    ADD COLUMN config_name     VARCHAR(128) NULL COMMENT '配置唯一标识' AFTER id,
    ADD COLUMN config_type     VARCHAR(16)  NOT NULL DEFAULT 'chat' COMMENT 'chat|embedding' AFTER provider,
    ADD COLUMN model_name      VARCHAR(128) NOT NULL DEFAULT '' COMMENT '上游模型型号' AFTER config_type,
    ADD COLUMN base_url        VARCHAR(512) DEFAULT '' COMMENT '接口 Base URL' AFTER model_name,
    ADD COLUMN api_key_cipher  TEXT COMMENT 'API Key SM4 密文' AFTER base_url,
    ADD COLUMN is_default      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '同类型默认模型' AFTER temperature,
    ADD COLUMN reasoning_effort VARCHAR(16) NOT NULL DEFAULT 'low' COMMENT '思考强度 low|medium|high|max' AFTER is_default,
    ADD COLUMN dimension       INT          NOT NULL DEFAULT 0 COMMENT 'Embedding 向量维度' AFTER reasoning_effort,
    ADD COLUMN sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序权重' AFTER dimension;

UPDATE ai_model_config
SET config_name = model_key,
    model_name  = model_key,
    is_default  = IF(priority = 1, 1, 0),
    sort_order  = priority
WHERE config_name IS NULL OR config_name = '';

ALTER TABLE ai_model_config
    MODIFY COLUMN config_name VARCHAR(128) NOT NULL COMMENT '配置唯一标识';

ALTER TABLE ai_model_config
    ADD UNIQUE KEY uk_config_name (config_name);

INSERT IGNORE INTO ai_model_config (
    config_name, model_key, provider, config_type, model_name, base_url,
    enabled, priority, fallback_model_key, max_tokens, temperature,
    is_default, reasoning_effort, dimension, sort_order
) VALUES (
    'bge-large-zh', 'bge-large-zh', 'bge', 'embedding', 'bge-large-zh-v1.5', 'http://127.0.0.1:8080/v1',
    1, 1, NULL, 512, 0.00,
    1, 'low', 1024, 1
);
