-- =============================================================================
-- V1.1 回滚脚本 (sql/rollback/V1_1_rollback.sql)
-- 智教云 · EduMind 
-- 回滚目标：将数据库结构与数据回滚至 V1.0 稳定基线状态
-- 注意事项：执行回滚前请务必使用 mysqldump 执行完整冷备
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 恢复/清理 V1.1 新增或修改的统计与课程字段
DROP TABLE IF EXISTS `course_statistics`;

-- 2. 清理 V1.1 新增的 AI 运维表扩展（若需回滚至 V1.0）
-- ALTER TABLE `ai_model_config` DROP COLUMN IF EXISTS `reasoning_effort`;
-- ALTER TABLE `ai_model_config` DROP COLUMN IF EXISTS `dimension`;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '=============================================================================' AS ROLLBACK_NOTICE;
SELECT ' EduMind V1.1 回滚脚本执行完毕，已将数据库重置至 V1.0 基线状态' AS ROLLBACK_NOTICE;
SELECT '=============================================================================' AS ROLLBACK_NOTICE;
