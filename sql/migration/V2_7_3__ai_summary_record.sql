-- =============================================================================
-- V2.7.3 AI 智能总结记录表（ai_summary_record）
--
-- 背景：
--   原 /api/ai/summary 只是一次性 LLM 调用，结果既不落库也不带来源信息，
--   用户关掉页面即丢失，既无法复盘、导出，也无法按课程沉淀教学资料。
--
-- 方案：
--   新增 ai_summary_record 记录每次总结的来源（知识库文档 / 自由文本）、
--   模式（全文速览 / 章节要点 / 易错清单 / 复习精要）、正文与统计信息，
--   支撑「历史记录 / 详情回看 / 重命名 / 删除 / 导出」完整闭环。
--
-- 幂等：CREATE TABLE IF NOT EXISTS 可重复执行，重复运行不报错、不丢数据。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 edumind < sql/migration/V2_7_2__ai_summary_record.sql
-- =============================================================================

USE edumind;

CREATE TABLE IF NOT EXISTS ai_summary_record (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT       DEFAULT NULL COMMENT '租户ID',
    user_id        BIGINT       NOT NULL COMMENT '创建用户ID',
    course_id      BIGINT       DEFAULT NULL COMMENT '关联课程ID（可选，用于课程上下文与筛选）',
    source_type    VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT '来源类型：DOCUMENT(知识库文档) / TEXT(自由文本)',
    document_id    BIGINT       DEFAULT NULL COMMENT '知识库文档ID（source_type=DOCUMENT 时）',
    document_name  VARCHAR(255) DEFAULT NULL COMMENT '来源文档/文本名称快照',
    summary_mode   VARCHAR(24)  NOT NULL DEFAULT 'OVERVIEW' COMMENT '总结模式：OVERVIEW/CHAPTER/MISTAKE/REVIEW',
    title          VARCHAR(255) NOT NULL COMMENT '总结标题',
    source_excerpt TEXT         DEFAULT NULL COMMENT '原始资料摘要片段（前 500 字）',
    content        MEDIUMTEXT   DEFAULT NULL COMMENT '总结正文（Markdown，最长 16MB）',
    source_length  INT          DEFAULT 0 COMMENT '原始资料字符数',
    word_count     INT          DEFAULT 0 COMMENT '总结正文有效字符数（去空白）',
    create_time    DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_summary_user_course (user_id, course_id),
    KEY idx_summary_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 智能总结记录';
