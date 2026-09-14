-- =============================================================================
-- V2.0.3 国密 KMS 密钥版本管理与长期记忆加密接入 (Gate I9)
-- 智教云 · EduMind
-- 可重复执行：列/权限已存在时自动跳过
-- =============================================================================

USE edumind;

-- 1. ai_memory_item 长期记忆敏感数据追加 SM4 密钥版本跟踪列
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'ai_memory_item' AND column_name = 'key_version'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ai_memory_item ADD COLUMN key_version INT NOT NULL DEFAULT 1 COMMENT ''SM4加密密钥版本'' AFTER content_ciphertext',
    'SELECT ''skip: ai_memory_item.key_version'' AS migration_note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 插入国密密钥管理查看与轮换权限点（按 code 幂等）
INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id)
VALUES
  ('security:key:view',   '国密密钥版本查看', 0),
  ('security:key:rotate', '国密密钥版本轮换', 0);

UPDATE sys_permission SET permission_name = '国密密钥版本查看', parent_id = 0 WHERE permission_code = 'security:key:view';
UPDATE sys_permission SET permission_name = '国密密钥版本轮换', parent_id = 0 WHERE permission_code = 'security:key:rotate';

-- 3. 授权系统管理员(ADMIN)与租户管理员(TENANT_ADMIN)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r
INNER JOIN sys_permission p ON p.permission_code IN ('security:key:view', 'security:key:rotate')
WHERE r.role_code IN ('ADMIN', 'TENANT_ADMIN');

-- 4. 历史元数据口径统一：SM4_CBC -> SM4_GCM（与 Sm4GcmService 实现一致）
UPDATE security_key_version SET algorithm = 'SM4_GCM' WHERE algorithm = 'SM4_CBC';
