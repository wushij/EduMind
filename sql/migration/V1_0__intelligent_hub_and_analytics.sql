-- =============================================================================
-- V1.0 ~ V1.1 智能教学中枢与学情分析（合并迁移）
-- 智教云 · EduMind
--
-- 合并来源：
--   · V1_0_0__intelligent_hub.sql
--   · V1_0_1__agent_tool_permission.sql
--   · V1_1_0__ai_call_log_course_id.sql
--   · V1_1_1__course_statistics_job.sql
--
-- 执行：mysql -u root -p edumind < sql/migration/V1_0__intelligent_hub_and_analytics.sql
-- 全新建库请直接执行 sql/init.sql，无需再跑本脚本。
-- =============================================================================

USE edumind;

-- -----------------------------------------------------------------------------
-- [V1_0_0__intelligent_hub.sql]
-- -----------------------------------------------------------------------------
-- =============================================================================
-- V1.0 智能教学中枢 — 单文件增量迁移
-- 智教云 · EduMind
--
-- 合并内容：
--   · 学情与掌握度（learning_record / knowledge_mastery / wrong_question_record / course_statistics）
--   · 知识图谱关系（knowledge_point_relation）
--   · AI Gateway（ai_model_config / ai_gateway_route + 默认路由种子）
--   · Agent 执行（agent_run / agent_step / agent_tool_call）
--
-- 适用：已执行至 V0_5_1__user_preferences（或同等结构）的已有库升级
-- 执行：mysql -u root -p edumind < sql/migration/V1_0_0__intelligent_hub.sql
--
-- 全新建库请直接执行 sql/init.sql，无需再跑本脚本。
-- Gate E2E 种子请另行执行：sql/migration/R__gate_e2e_seeds.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 学情与掌握度
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS learning_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    student_id      BIGINT       NOT NULL,
    course_id       BIGINT       NOT NULL,
    action_type     VARCHAR(32)  NOT NULL COMMENT 'LOGIN/STUDY/RESOURCE_VIEW/AI_CHAT',
    duration_minutes INT         DEFAULT 0,
    resource_id     BIGINT       DEFAULT NULL,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_student_course (student_id, course_id),
    KEY idx_course_time (course_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习行为明细';

CREATE TABLE IF NOT EXISTS knowledge_mastery (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    student_id          BIGINT        NOT NULL,
    course_id           BIGINT        NOT NULL,
    knowledge_point_id  BIGINT        NOT NULL,
    mastery_score       DECIMAL(5,4)  NOT NULL DEFAULT 0,
    sample_count        INT           NOT NULL DEFAULT 0,
    last_assessed_at    DATETIME      DEFAULT NULL,
    create_time         DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_kp (student_id, knowledge_point_id),
    KEY idx_course_kp (course_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点掌握度';

CREATE TABLE IF NOT EXISTS wrong_question_record (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    student_id          BIGINT       NOT NULL,
    course_id           BIGINT       NOT NULL,
    question_id         BIGINT       NOT NULL,
    knowledge_point_id  BIGINT       DEFAULT NULL,
    error_types         VARCHAR(128) DEFAULT NULL COMMENT 'CONCEPT,LOGIC,CALC',
    diagnosis           VARCHAR(512) DEFAULT NULL,
    variant_question_ids VARCHAR(256) DEFAULT NULL,
    wrong_count         INT          NOT NULL DEFAULT 1,
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course_question (course_id, question_id),
    KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题记录';

CREATE TABLE IF NOT EXISTS course_statistics (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    course_id       BIGINT        NOT NULL,
    stat_date       DATE          NOT NULL,
    active_users    INT           DEFAULT 0,
    avg_score       DECIMAL(5,2)  DEFAULT NULL,
    completion_rate DECIMAL(5,4)  DEFAULT NULL,
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_date (course_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程日聚合';

-- -----------------------------------------------------------------------------
-- 2. 知识图谱关系
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS knowledge_point_relation (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    source_knowledge_point_id BIGINT       NOT NULL,
    target_knowledge_point_id BIGINT       NOT NULL,
    relation_type           VARCHAR(32)  NOT NULL COMMENT 'prerequisite/successor/related/assessed_by/supported_by',
    properties              JSON         DEFAULT NULL,
    create_time             DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_relation (source_knowledge_point_id, target_knowledge_point_id, relation_type),
    KEY idx_source (source_knowledge_point_id),
    KEY idx_target (target_knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点关系边';

-- -----------------------------------------------------------------------------
-- 3. AI Gateway
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ai_model_config (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    model_key           VARCHAR(64)  NOT NULL,
    provider            VARCHAR(32)  NOT NULL,
    enabled             TINYINT(1)   NOT NULL DEFAULT 1,
    priority            INT          NOT NULL DEFAULT 1,
    fallback_model_key  VARCHAR(64)  DEFAULT NULL,
    max_tokens          INT          DEFAULT 4096,
    temperature         DECIMAL(3,2) DEFAULT 0.70,
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_key (model_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型配置';

CREATE TABLE IF NOT EXISTS ai_gateway_route (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    scene               VARCHAR(32)  NOT NULL COMMENT 'CHAT/RAG/AGENT/GRADING',
    primary_model_key   VARCHAR(64)  NOT NULL,
    fallback_model_key  VARCHAR(64)  DEFAULT NULL,
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scene (scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Gateway 场景路由';

INSERT IGNORE INTO ai_model_config (model_key, provider, enabled, priority, fallback_model_key, max_tokens, temperature) VALUES
('deepseek-chat', 'deepseek', 1, 1, 'mock', 4096, 0.70),
('qwen-turbo', 'qwen', 1, 2, 'mock', 4096, 0.70),
('mock', 'mock', 1, 99, NULL, 4096, 0.70);

INSERT IGNORE INTO ai_gateway_route (scene, primary_model_key, fallback_model_key) VALUES
('CHAT', 'deepseek-chat', 'mock'),
('RAG', 'deepseek-chat', 'mock'),
('AGENT', 'deepseek-chat', 'mock'),
('GRADING', 'deepseek-chat', 'mock');

-- -----------------------------------------------------------------------------
-- 4. Agent 执行
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS agent_run (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    run_id          VARCHAR(64)  NOT NULL,
    agent_code      VARCHAR(32)  NOT NULL,
    user_id         BIGINT       NOT NULL,
    course_id       BIGINT       DEFAULT NULL,
    goal            TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING',
    result_json     JSON         DEFAULT NULL,
    model_key       VARCHAR(64)  DEFAULT NULL,
    token_usage     INT          DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_id (run_id),
    KEY idx_user (user_id),
    KEY idx_agent (agent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 执行实例';

CREATE TABLE IF NOT EXISTS agent_step (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    run_id          VARCHAR(64)  NOT NULL,
    step_index      INT          NOT NULL,
    step_type       VARCHAR(16)  NOT NULL COMMENT 'INTENT/PLAN/TOOL/LLM/RESULT',
    title           VARCHAR(256) NOT NULL,
    tool_name       VARCHAR(64)  DEFAULT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    input_preview   TEXT         DEFAULT NULL,
    output_preview  TEXT         DEFAULT NULL,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_run (run_id, step_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 步骤';

CREATE TABLE IF NOT EXISTS agent_tool_call (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    run_id          VARCHAR(64)  NOT NULL,
    step_id         BIGINT       DEFAULT NULL,
    tool_name       VARCHAR(64)  NOT NULL,
    input_json      JSON         DEFAULT NULL,
    output_json     JSON         DEFAULT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING',
    duration_ms     INT          DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_run_tool (run_id, tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tool 调用日志';


-- -----------------------------------------------------------------------------
-- [V1_0_1__agent_tool_permission.sql]
-- -----------------------------------------------------------------------------
-- Agent 中心 / Tool Calling 权限（修复管理员访问 /api/ai/agents 返回 403）
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(31, 'ai:tool:use', 'Agent工具调用', 0);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code = 'ai:tool:use';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code = 'ai:tool:use';


-- -----------------------------------------------------------------------------
-- [V1_1_0__ai_call_log_course_id.sql]
-- -----------------------------------------------------------------------------
-- V1.1.0 审计表增加课程上下文维度 (支持按课程维度精确统计 AI 耗用)
-- 说明：若已通过 init.sql 全量初始化，ai_call_log.course_id 已存在，本脚本会自动跳过

SET @db_name = DATABASE();

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'ai_call_log'
      AND COLUMN_NAME = 'course_id'
);

SET @add_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE ai_call_log ADD COLUMN course_id BIGINT NULL COMMENT ''关联课程ID (0或NULL表示全局/无课程上下文)'' AFTER user_id',
    'SELECT ''skip: ai_call_log.course_id already exists'' AS migration_note'
);
PREPARE stmt FROM @add_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'ai_call_log'
      AND INDEX_NAME = 'idx_call_log_course_time'
);

SET @add_index_sql = IF(
    @index_exists = 0,
    'ALTER TABLE ai_call_log ADD INDEX idx_call_log_course_time (course_id, create_time)',
    'SELECT ''skip: idx_call_log_course_time already exists'' AS migration_note'
);
PREPARE stmt FROM @add_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- -----------------------------------------------------------------------------
-- [V1_1_1__course_statistics_job.sql]
-- -----------------------------------------------------------------------------
-- V1.1.1 课程学情日聚合表 (PRD §34 / 报表高性能查询支撑)
CREATE TABLE IF NOT EXISTS course_statistics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  stat_date DATE NOT NULL COMMENT '统计日期 (YYYY-MM-DD)',
  student_count INT NOT NULL DEFAULT 0 COMMENT '当日活跃学生数',
  avg_score DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '班级均分',
  mastery_avg DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '班级知识点平均掌握度(0.0000~1.0000)',
  ai_call_count INT NOT NULL DEFAULT 0 COMMENT '当日AI助教调用总量',
  wrong_count INT NOT NULL DEFAULT 0 COMMENT '当日新增错题记录数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_course_date (course_id, stat_date),
  INDEX idx_stat_date (stat_date),
  INDEX idx_course_stat (course_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程学情日聚合统计表';


