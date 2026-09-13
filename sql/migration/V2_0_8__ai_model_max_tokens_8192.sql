-- Chat 模型默认 max_tokens 提升至 8192（思考与正文共用输出预算）
UPDATE ai_model_config
SET max_tokens = 8192
WHERE config_type = 'chat'
  AND (max_tokens IS NULL OR max_tokens = 4096);

ALTER TABLE ai_model_config
    MODIFY COLUMN max_tokens INT DEFAULT 8192 COMMENT '最大 Token';
