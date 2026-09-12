-- =============================================================================
-- V0.5 产品增强版 — 单文件增量迁移
-- 智教云 · EduMind
--
-- 合并内容：
--   · 知识库 Chunk 切片
--   · 向量索引任务与 Chunk-Vector 映射
--   · Prompt 模板治理 / AI 审计扩展 / AI 配额
--   · 系统全局配置（邮件 SMTP 等）
--   · RAG / Prompt / 审计 / 配额权限 seed
--
-- 适用：已执行至 V0_2_0__mvp_expansion（或同等结构）的已有库升级
-- 执行：mysql -u root -p edumind < sql/migration/V0_5_0__product_enhancement.sql
--
-- 全新建库请直接执行 sql/init.sql，无需再跑本脚本。
-- =============================================================================

USE edumind;

-- -----------------------------------------------------------------------------
-- 1. 知识库 Chunk 切片
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS knowledge_document_chunk (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Chunk ID',
    document_id       BIGINT       NOT NULL COMMENT '所属文档ID',
    knowledge_base_id BIGINT       NOT NULL COMMENT '所属知识库ID（冗余便于过滤）',
    chunk_index       INT          NOT NULL DEFAULT 0 COMMENT '切片序号',
    content           TEXT         NOT NULL COMMENT '切片正文',
    page_no           INT          DEFAULT NULL COMMENT '页码（PDF）',
    heading           VARCHAR(256) DEFAULT NULL COMMENT '章节标题',
    char_count        INT          DEFAULT 0 COMMENT '字符数',
    token_estimate    INT          DEFAULT 0 COMMENT 'Token 估算',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_document_id (document_id),
    KEY idx_kb_id (knowledge_base_id),
    KEY idx_doc_chunk_index (document_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档切片表';

-- knowledge_base 扩展字段（已有库升级；若列已存在会报错，可忽略）
ALTER TABLE knowledge_base
    ADD COLUMN chunk_count INT DEFAULT 0 COMMENT 'Chunk 总数' AFTER document_count;

ALTER TABLE knowledge_base
    ADD COLUMN index_status VARCHAR(32) DEFAULT 'PENDING' COMMENT '向量索引状态 PENDING/INDEXING/INDEXED/INDEX_FAILED' AFTER chunk_count;

-- -----------------------------------------------------------------------------
-- 2. 向量索引任务与 Chunk 向量映射
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS knowledge_index_task (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    knowledge_base_id BIGINT       NOT NULL COMMENT '知识库ID',
    mode              VARCHAR(16)  NOT NULL DEFAULT 'FULL' COMMENT 'FULL/INCREMENTAL',
    status            VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/INDEXING/INDEXED/INDEX_FAILED',
    total_chunks      INT          DEFAULT 0 COMMENT '总 Chunk 数',
    indexed_chunks    INT          DEFAULT 0 COMMENT '已索引 Chunk 数',
    failed_chunks     INT          DEFAULT 0 COMMENT '失败 Chunk 数',
    embedding_model   VARCHAR(64)  DEFAULT NULL COMMENT 'Embedding 模型',
    error_message     TEXT         DEFAULT NULL COMMENT '错误信息',
    started_at        DATETIME     DEFAULT NULL COMMENT '开始时间',
    finished_at       DATETIME     DEFAULT NULL COMMENT '结束时间',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_kb_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库向量索引任务表';

CREATE TABLE IF NOT EXISTS knowledge_chunk_index (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    chunk_id          BIGINT       NOT NULL COMMENT 'Chunk ID',
    knowledge_base_id BIGINT       NOT NULL COMMENT '知识库ID',
    document_id       BIGINT       NOT NULL COMMENT '文档ID',
    vector_id         VARCHAR(64)  NOT NULL COMMENT '向量库中的向量ID',
    embed_status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/INDEXED/FAILED',
    embedding_model   VARCHAR(64)  DEFAULT NULL COMMENT 'Embedding 模型',
    error_message     VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_chunk_id (chunk_id),
    KEY idx_kb_id (knowledge_base_id),
    KEY idx_doc_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chunk 向量索引映射表';

-- -----------------------------------------------------------------------------
-- 3. Prompt 治理 / AI 消息引用 / 调用日志扩展 / AI 配额
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS prompt_template (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    code         VARCHAR(64)  NOT NULL COMMENT '模板编码',
    name         VARCHAR(128) NOT NULL COMMENT '模板名称',
    category     VARCHAR(32)  DEFAULT NULL COMMENT '分类',
    status       VARCHAR(16)  DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
    version      INT          DEFAULT 1 COMMENT '当前版本号',
    content      TEXT         NOT NULL COMMENT '模板内容',
    variables    VARCHAR(512) DEFAULT NULL COMMENT '变量列表（逗号分隔）',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt 模板表';

CREATE TABLE IF NOT EXISTS prompt_template_version (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    template_id  BIGINT       NOT NULL COMMENT '模板ID',
    version      INT          NOT NULL COMMENT '版本号',
    content      TEXT         NOT NULL COMMENT '版本内容',
    variables    VARCHAR(512) DEFAULT NULL COMMENT '变量列表',
    published_by BIGINT       DEFAULT NULL COMMENT '发布人',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_version (template_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt 模板版本表';

ALTER TABLE ai_message
    ADD COLUMN citations_json TEXT DEFAULT NULL COMMENT '引用 JSON' AFTER content;

ALTER TABLE ai_call_log
    ADD COLUMN knowledge_base_id BIGINT DEFAULT NULL COMMENT '知识库ID' AFTER scene,
    ADD COLUMN retrieval_hit_count INT DEFAULT 0 COMMENT '检索命中数' AFTER knowledge_base_id,
    ADD COLUMN citation_doc_ids VARCHAR(512) DEFAULT NULL COMMENT '引用文档ID列表' AFTER retrieval_hit_count;

CREATE TABLE IF NOT EXISTS sys_ai_quota (
    id                BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id           BIGINT NOT NULL COMMENT '用户ID',
    daily_token_limit INT    DEFAULT 0 COMMENT '日 Token 上限，0 表示不限',
    daily_call_limit  INT    DEFAULT 0 COMMENT '日调用上限，0 表示不限',
    used_tokens_today INT    DEFAULT 0 COMMENT '今日已用 Token',
    used_calls_today  INT    DEFAULT 0 COMMENT '今日已调用次数',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 配额表';

INSERT INTO prompt_template (code, name, category, status, version, content, variables) VALUES
('chat_rag', '课程AI-RAG对话', 'rag', 'PUBLISHED', 1,
 '你是课程 AI 助教。请基于以下资料回答用户问题。\n\n【参考资料】\n{{context}}\n\n【用户问题】\n{{question}}',
 'context,question')
ON DUPLICATE KEY UPDATE name = VALUES(name), content = VALUES(content), status = 'PUBLISHED';

-- -----------------------------------------------------------------------------
-- 4. 系统全局配置（邮件 SMTP 等）与用户邮箱索引
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置主键ID',
    config_key   VARCHAR(64)  NOT NULL UNIQUE COMMENT '配置键（唯一标示）',
    config_value LONGTEXT     DEFAULT NULL COMMENT '配置值（支持JSON或长文本）',
    config_name  VARCHAR(128) NOT NULL COMMENT '配置中文名称',
    config_group VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT' COMMENT '配置分组（mail/security/base等）',
    remark       VARCHAR(255) DEFAULT NULL COMMENT '配置备注说明',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_config_group (config_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统全局参数配置表';

INSERT INTO sys_config (config_key, config_value, config_name, config_group, remark) VALUES
('sys.mail.config', '{"enabled":false,"host":"smtp.qq.com","port":465,"username":"","password":"","fromName":"智教云 · EduMind","useSsl":true,"codeExpireMinutes":5,"codeIntervalSeconds":60,"dailyLimitPerEmail":10}', '邮件发送服务配置(SMTP)', 'mail', 'SMTP发信参数、发信人名称与验证码防刷策略'),
('sys.base.info', '{"platformName":"智教云 · EduMind","subTitle":"AI 智能教学赋能平台","copyright":"© 2026 EduMind. All rights reserved.","icp":"京ICP备20260001号-1"}', '平台基础信息配置', 'base', '平台站点标题、副标与备案声明')
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name), config_value = VALUES(config_value);

-- sys_user.email 索引（幂等）
SET @dbname = DATABASE();
SET @tablename = 'sys_user';
SET @keyname = 'idx_user_email';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = @keyname
  ) > 0,
  'SELECT 1',
  CONCAT('CREATE INDEX ', @keyname, ' ON ', @tablename, ' (email)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- -----------------------------------------------------------------------------
-- 5. V0.5 权限 seed（与 init.sql 字段名对齐）
-- -----------------------------------------------------------------------------

INSERT INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(25, 'knowledge:rag:debug', 'RAG调试', 0),
(26, 'system:prompt:view',  'Prompt查看', 0),
(27, 'system:prompt:edit',  'Prompt编辑', 0),
(28, 'system:audit:view',   'AI审计查看', 0),
(29, 'system:quota:view',   'AI配额查看', 0),
(30, 'system:quota:edit',   'AI配额编辑', 0)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 管理员拥有新增权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 25 AND 30;

-- 教师拥有 RAG 调试权限（不含 system:* 管理权限）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code = 'knowledge:rag:debug';
