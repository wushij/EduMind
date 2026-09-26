-- =============================================================================
-- V2.7.5: 清除 AI 工具广场「调用次数」里写死的演示基线
-- =============================================================================
-- 背景：V0.2 种子给工具卡片填了一组演示用调用次数（2436 / 1820 / 956 / 310 /
--       5200 / 1680），它们与真实调用无关，界面却按"真实统计"展示，属于假数据；
--       新工具（如 AI 智能备课）从 0 起真实累加，两者口径不一致。
-- 方案：扣掉各工具的演示基线，只保留真实累计（每次「立即使用」recordToolUse +1）。
--       条件 `use_count >= 基线` 保证幂等：重复执行、或全新库（种子基线已改为 0）
--       都自动变成空操作；即便真实用量日后涨到基线也几乎不可能误扣。
-- 同步：init.sql / R__seed_data.sql 的 use_count 已一并改为 0，新库不再出现假数字。
-- 幂等：可重复执行。
-- =============================================================================

UPDATE ai_tool SET use_count = use_count - 2436 WHERE id = 'tool_question_gen' AND use_count >= 2436;
UPDATE ai_tool SET use_count = use_count - 1820 WHERE id = 'tool_exam_gen'     AND use_count >= 1820;
UPDATE ai_tool SET use_count = use_count - 956  WHERE id = 'tool_grading'      AND use_count >= 956;
UPDATE ai_tool SET use_count = use_count - 310  WHERE id = 'tool_summary'      AND use_count >= 310;
UPDATE ai_tool SET use_count = use_count - 5200 WHERE id = 'tool_chat'         AND use_count >= 5200;
UPDATE ai_tool SET use_count = use_count - 1680 WHERE id = 'tool_practice'     AND use_count >= 1680;
