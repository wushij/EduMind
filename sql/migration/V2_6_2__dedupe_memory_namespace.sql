-- ============================================================
-- 修复 AI 长期记忆命名空间重复导致「记忆与隐私」页面 500
--
-- 背景：ai_memory_namespace 缺少 (tenant_id, user_id, course_id) 唯一约束。
--      MySQL 唯一索引中 NULL 不参与去重，而"个人全局空间"原先用 course_id = NULL 表示，
--      于是并发首访会各插一条全局空间；查询走 selectOne 遇到多行直接抛
--      TooManyResultsException → /api/ai/memories 返回 500。
--
-- 处理（可重复执行，顺序不可调换）：
--   1) 按 (tenant,user,课程) 分组收敛重复，NULL 与 0 视为同一个"全局空间"分组，
--      保留者优先取已是占位 0 的规范行，否则取最早创建的一条；
--   2) 条目迁移到保留者，避免历史记忆丢失；
--   3) 删除重复命名空间；
--   4) 剩余 NULL 全局空间统一为占位 0（此时已无冲突）；
--   5) 加唯一索引防止再次产生。
--
-- 应用侧对应：AiMemoryNamespaceEntity.GLOBAL_COURSE_ID / AgentMemoryServiceImpl.globalCourseId()
-- ============================================================

-- 1) 计算每个分组的保留者
DROP TEMPORARY TABLE IF EXISTS tmp_memory_ns_keep;
CREATE TEMPORARY TABLE tmp_memory_ns_keep AS
SELECT
    tenant_id,
    user_id,
    IFNULL(course_id, 0)                    AS course_key,
    MIN(CASE WHEN course_id = 0 THEN id END) AS placeholder_id,
    MIN(id)                                  AS min_id
FROM ai_memory_namespace
GROUP BY tenant_id, user_id, IFNULL(course_id, 0);

-- 2) 条目迁移到保留者
UPDATE ai_memory_item i
JOIN ai_memory_namespace n ON n.id = i.namespace_id
JOIN tmp_memory_ns_keep k
  ON k.tenant_id = n.tenant_id
 AND k.user_id = n.user_id
 AND k.course_key = IFNULL(n.course_id, 0)
SET i.namespace_id = IFNULL(k.placeholder_id, k.min_id)
WHERE i.namespace_id <> IFNULL(k.placeholder_id, k.min_id);

-- 3) 删除重复命名空间
DELETE n FROM ai_memory_namespace n
JOIN tmp_memory_ns_keep k
  ON k.tenant_id = n.tenant_id
 AND k.user_id = n.user_id
 AND k.course_key = IFNULL(n.course_id, 0)
WHERE n.id <> IFNULL(k.placeholder_id, k.min_id);

-- 4) 全局空间统一为占位 0（NULL 无法参与唯一索引去重）
UPDATE ai_memory_namespace SET course_id = 0 WHERE course_id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_memory_ns_keep;

-- 5) 唯一约束（已存在则跳过，保证脚本可重复执行）
SET @uk_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_memory_namespace'
      AND index_name = 'uk_tenant_user_course'
);
SET @uk_sql := IF(@uk_exists = 0,
    'ALTER TABLE ai_memory_namespace ADD UNIQUE KEY uk_tenant_user_course (tenant_id, user_id, course_id)',
    'SELECT ''uk_tenant_user_course already exists'' AS skip_msg');
PREPARE uk_stmt FROM @uk_sql;
EXECUTE uk_stmt;
DEALLOCATE PREPARE uk_stmt;
