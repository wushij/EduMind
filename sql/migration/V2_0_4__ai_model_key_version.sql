-- =============================================================================
-- V2.0.4 AI 模型 API Key 接入国密 KMS 与密钥版本管理 (Gate I10)
-- 智教云 · EduMind
-- =============================================================================

-- 1. ai_model_config 平台级配置表追加 SM4 API Key 密钥版本列
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'ai_model_config' AND column_name = 'key_version'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ai_model_config ADD COLUMN key_version INT NOT NULL DEFAULT 1 COMMENT ''API Key SM4密钥版本'' AFTER api_key_cipher',
    'SELECT ''skip: ai_model_config.key_version already exists'' AS migration_note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 注入平台全局 AI 模型密钥 KMS 种子记录（tenant_id=1, key_alias=edumind-model-key, SM4_GCM）
INSERT IGNORE INTO security_key_version (tenant_id, key_alias, key_version, algorithm, status)
VALUES (1, 'edumind-model-key', 1, 'SM4_GCM', 'ACTIVE');
