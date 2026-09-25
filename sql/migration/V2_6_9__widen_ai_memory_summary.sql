-- =============================================================================
-- V2.6.9 加宽 AI 长期记忆摘要字段（VARCHAR(512) → TEXT）
--
-- 背景（与 V2.6.8 同类的字段超长故障）：
--   ai_memory_item.summary 并不只是"用户手填的一行摘要"，它有三条写入来源：
--     1) 用户在「记忆与隐私」页手填摘要（MemoryItemCreateDTO / MemoryItemUpdateDTO）；
--     2) Agent 记忆提取：大模型生成的候选摘要，用户批量确认后直接落库
--        （confirmExtractedMemories，长度完全由模型输出决定，不可控）；
--     3) 记忆纠错反馈 MODIFY：用户提交的 correctContent 会直接覆盖 summary 落库
--        （ai_memory_feedback.correct_content 本身是 TEXT，长度上不设防）。
--   而 summary 仅 VARCHAR(512)，严格模式（STRICT_TRANS_TABLES）下超长不会被静默截断，
--   而是抛 "Data too long for column 'summary' at row 1"，被 GlobalExceptionHandler
--   映射为 HTTP 400「数据操作冲突，请检查后重试」——现象与错题 AI 诊断一模一样。
--
-- 方案：
--   1) 列宽提升为 TEXT（最长 65535 字节，utf8mb4 下可容纳约 1.6 万汉字）；
--   2) 应用侧由 MemorySummaryNormalizer 统一做「1000 字长度收口 + 按句边界截断」，
--      覆盖上述全部写入路径，任何一条来源超长都不会再变成 400；
--   3) MemoryItemCreateDTO / MemoryItemUpdateDTO 增加 @Size(max = 1000) 前置校验，
--      用户手填超长时返回明确的字段级提示，而不是落到数据库层报错。
--
-- 说明：summary 不参与任何索引、唯一键与 SQL 聚合（去重逻辑在应用层做字符串比对），
--       因此由 VARCHAR 提升为 TEXT 不影响既有查询计划与唯一性约束。
--
-- 幂等：MODIFY COLUMN 可重复执行，重复运行不报错、不丢数据。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 edumind < sql/migration/V2_6_9__widen_ai_memory_summary.sql
-- =============================================================================

USE edumind;

ALTER TABLE ai_memory_item
    MODIFY COLUMN summary TEXT NOT NULL COMMENT '记忆摘要内容(明文脱敏，长文本上限由应用层控制)';
