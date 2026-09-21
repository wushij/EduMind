-- ============================================================
-- 调用审计补充「命中的模型配置键」
--
-- 背景：ai_call_log.model 记录的是上游型号（model_name），
--      而多条模型配置可以指向同一个上游型号（例如配置 v4.1flash 与 Flash 的
--      model_name 都是 deepseek-v4-flash），于是调用明细按型号聚合后只剩一行，
--      无法判断实际命中哪条配置，表现为"我选的是 v4.1flash，看着却像用了 v4flash"。
--
-- 应用侧对应：AiCallAuditServiceImpl 写入 model_key；网关指标按 model_key 分组。
-- 历史数据 model_key 为空，查询时回退按 model 分组，无需回填。
-- ============================================================

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_call_log'
      AND column_name = 'model_key'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE ai_call_log ADD COLUMN model_key VARCHAR(64) NULL COMMENT ''命中的模型配置路由键'' AFTER model',
    'SELECT ''ai_call_log.model_key already exists'' AS skip_msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
