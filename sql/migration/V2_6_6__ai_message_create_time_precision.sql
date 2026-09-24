-- ============================================================
-- ai_message.create_time 提升到毫秒精度（多轮上下文排序稳定性）
--
-- 背景：ai_message.create_time 原本是秒级 DATETIME（DEFAULT CURRENT_TIMESTAMP），
--      而 MessageDao.listByConversationId / listRecentByConversationId /
--      findLastByConversationIdAndRole 都只按 create_time 排序。
--      同一秒内写入多条消息时排序结果不确定，可能把 assistant 排到 user 之前，
--      使 ChatHistoryBuilder 组装出的多轮上下文角色交替错乱。
--
-- 应用侧对应：ChatServiceImpl.saveMessage 与 GlobalAssistantServiceImpl 落库时
--      显式写入 LocalDateTime.now()；一轮问答中 user 落库与 assistant 落库之间
--      夹着整段模型调用，耗时远大于 1ms，毫秒精度足以唯一确定先后顺序。
--
-- 说明：ai_message.id 由 ASSIGN_UUID 生成，是随机 UUID，不具备时序语义，
--      不能作为 create_time 的二级排序键，因此这里通过提升时间精度解决。
--
-- 幂等：仅当当前精度低于毫秒时才执行 MODIFY，可重复执行。
-- ============================================================

SET @col_precision := (
    SELECT DATETIME_PRECISION FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_message'
      AND column_name = 'create_time'
);

SET @ddl := IF(@col_precision IS NOT NULL AND @col_precision < 3,
    'ALTER TABLE ai_message MODIFY COLUMN create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT ''发送时间（毫秒精度，保证多轮消息排序稳定）''',
    'SELECT ''ai_message.create_time already has millisecond precision'' AS skip_msg');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
