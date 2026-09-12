-- =============================================================================
-- V0.2 MVP 扩展版 — 单文件增量迁移
-- 智教云 · EduMind
--
-- 合并内容：
--   · 题库与题库题目关联
--   · 作业 / 作答 / AI 批改
--   · 知识库文档与文本切片
--   · RBAC 权限 / AI 调用日志 / 课程资源关联
--   · 系统通知 / 学情统计快照
--   · 种子账号密码修复
--   · AI 工具广场元数据扩展与 14 项工具 seed
--
-- 适用：已执行 V0_1_0__mvp_core.sql 的库升级
-- 执行：mysql -u root -p edumind < sql/migration/V0_2_0__mvp_expansion.sql
--
-- 全新建库请直接执行 sql/init.sql，无需再跑本脚本。
-- =============================================================================

USE edumind;

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. 题库
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS question_bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    course_id BIGINT,
    description TEXT,
    question_count INT DEFAULT 0,
    status INT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';

CREATE TABLE IF NOT EXISTS question_bank_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bank_question (bank_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库题目关联表';

-- -----------------------------------------------------------------------------
-- 2. 作业与 AI 批改
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    exam_id BIGINT,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    deadline DATETIME,
    status VARCHAR(16) DEFAULT 'DRAFT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

CREATE TABLE IF NOT EXISTS assignment_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(16) DEFAULT 'IN_PROGRESS',
    total_score INT,
    max_score INT,
    submit_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_assignment_student (assignment_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

CREATE TABLE IF NOT EXISTS submission_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业作答表';

CREATE TABLE IF NOT EXISTS grading_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score INT DEFAULT 0,
    max_score INT DEFAULT 0,
    is_correct TINYINT,
    ai_comment TEXT,
    teacher_comment TEXT,
    status VARCHAR(32) DEFAULT 'PENDING',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业批改判定表';

-- -----------------------------------------------------------------------------
-- 3. 知识库文档
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS knowledge_document (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    knowledge_base_id BIGINT       NOT NULL COMMENT '所属知识库ID',
    file_name         VARCHAR(256) NOT NULL COMMENT '文件名称',
    file_type         VARCHAR(32)  DEFAULT NULL COMMENT '文件类型（PDF/WORD/MD/TXT）',
    file_size         BIGINT       DEFAULT 0 COMMENT '文件字节大小',
    object_key        VARCHAR(512) DEFAULT NULL COMMENT 'MinIO/OSS对象存储路径',
    parse_status      VARCHAR(16)  DEFAULT 'PENDING' COMMENT '解析状态',
    error_message     TEXT         DEFAULT NULL COMMENT '错误信息',
    status            INT          DEFAULT 1 COMMENT '状态（1-正常 0-禁用）',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_kb_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

CREATE TABLE IF NOT EXISTS knowledge_document_text (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '切片文本ID',
    document_id BIGINT   NOT NULL UNIQUE COMMENT '所属文档ID',
    content     LONGTEXT DEFAULT NULL COMMENT '切片提取正文/纯文本内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doc_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库切片纯文本表';

-- -----------------------------------------------------------------------------
-- 4. RBAC / AI 审计 / 课程资源关联
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(64) NOT NULL UNIQUE,
    permission_name VARCHAR(128) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS ai_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    model VARCHAR(64),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    latency_ms INT DEFAULT 0,
    scene VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型调用日志表';

CREATE TABLE IF NOT EXISTS course_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    resource_id BIGINT,
    document_id BIGINT,
    title VARCHAR(128),
    resource_type VARCHAR(32),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程资源关联表';

-- -----------------------------------------------------------------------------
-- 5. 通知与统计
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    content TEXT,
    type VARCHAR(32) DEFAULT 'SYSTEM',
    is_read TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息通知表';

CREATE TABLE IF NOT EXISTS statistics_daily_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL,
    course_id BIGINT DEFAULT NULL,
    active_student_count INT DEFAULT 0,
    total_ai_conversations INT DEFAULT 0,
    total_tokens_consumed BIGINT DEFAULT 0,
    avg_score DOUBLE DEFAULT NULL,
    KEY idx_stat_date (stat_date),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日学情与AI统计快照表';

-- -----------------------------------------------------------------------------
-- 6. 种子账号密码修复（旧哈希无法校验 admin123）
-- -----------------------------------------------------------------------------

UPDATE sys_user
SET password = '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq'
WHERE username IN ('admin', 'teacher', 'student', 'student2');

-- -----------------------------------------------------------------------------
-- 7. AI 工具广场元数据扩展 + 14 项工具 seed
-- -----------------------------------------------------------------------------

ALTER TABLE ai_tool
    ADD COLUMN detailed_intro VARCHAR(1024) NULL COMMENT '详情页长描述' AFTER description;

ALTER TABLE ai_tool
    ADD COLUMN model_id VARCHAR(64) NULL COMMENT '默认模型' AFTER icon;

ALTER TABLE ai_tool
    ADD COLUMN is_hot TINYINT DEFAULT 0 COMMENT '热门标记（1-是 0-否）' AFTER is_recommended;

ALTER TABLE ai_tool
    ADD COLUMN execution_mode VARCHAR(16) DEFAULT 'ROUTE' COMMENT 'ROUTE|V05_NOTICE' AFTER route;

INSERT INTO ai_tool (id, name, description, detailed_intro, category, icon, model_id, route, execution_mode, tags, is_recommended, is_hot, use_count, status) VALUES
('tool_question_gen', 'AI 智能出题', '根据课程、章节和知识点智能生成高质量题目', '支持按章节与知识点勾选范围，配置题型、难度与题量后批量生成结构化试题，并可一键入库。', 'TEACHER', 'EditPen', 'deepseek-chat', '/ai/question/generate', 'ROUTE', '出题,教师,热门', 1, 1, 2436, 1),
('tool_exam_gen', 'AI 智能组卷', '按总分、题型比例与难度规则快速生成标准化试卷', '内置总分校验与题型配比引擎，支持预览换题、调分并保存为可复用试卷。', 'TEACHER', 'Document', 'deepseek-chat', '/ai/exam/generate', 'ROUTE', '组卷,教师', 1, 1, 1820, 1),
('tool_grading', 'AI 智能批改', '客观题秒级判分，主观题 AI 评分与评语生成', '支持作业提交后自动批改与教师复核改分，减轻期末阅卷压力。', 'TEACHER', 'Checked', 'deepseek-chat', '/ai/grading', 'ROUTE', '批改,教师', 1, 0, 956, 1),
('tool_lesson', 'AI 教案生成', '输入授课主题与学时，生成结构化教案与课堂设计', '覆盖教学目标、重难点、课堂互动与板书建议，辅助青年教师快速备课。', 'TEACHER', 'Notebook', 'deepseek-chat', '/ai/marketplace/v05/tool_lesson', 'V05_NOTICE', '教案,教师', 0, 0, 420, 1),
('tool_summary', 'AI 课程总结', '按章节或知识模块提炼核心要点与易错清单', '支持长文档与课件要点结构化摘要，生成考前复习精要。', 'TEACHER', 'DataAnalysis', 'deepseek-chat', '/ai/marketplace/v05/tool_summary', 'V05_NOTICE', '总结,知识提炼', 0, 0, 310, 1),
('tool_chat', 'AI 课程问答', '基于课程资料的上下文助教答疑（SSE 流式）', '在课程空间内多轮对话，支持 Markdown、公式与代码高亮渲染。', 'GENERAL', 'ChatDotRound', 'deepseek-chat', '/course/101/ai', 'ROUTE', '问答,助教,热门', 1, 1, 5200, 1),
('tool_wrong_analysis', 'AI 错题分析', '针对错题给出思路引导、错误归因与变式练习', '结合学生作答记录分析错因类型，并推荐巩固练习方向。', 'STUDENT', 'Warning', 'deepseek-chat', '/ai/marketplace/v05/tool_wrong_analysis', 'V05_NOTICE', '错题,学生', 0, 0, 680, 1),
('tool_knowledge_explain', 'AI 知识点讲解', '由浅入深讲解核心概念，支持苏格拉底式引导', '针对单个知识点提供类比、例题与追问，帮助学生建立直觉理解。', 'STUDENT', 'Reading', 'deepseek-chat', '/ai/marketplace/v05/tool_knowledge_explain', 'V05_NOTICE', '讲解,学生', 0, 0, 890, 1),
('tool_practice', 'AI 自适应刷题', '根据薄弱知识点智能生成阶梯练习', '分析近期学习数据，推送专项巩固题包与难度递进练习。', 'STUDENT', 'Reading', 'deepseek-chat', '/learning/recommendations', 'ROUTE', '练习,学生,推荐', 1, 0, 1680, 1),
('tool_learning_plan', 'AI 学习计划', '根据学情报告自动定制复习日程', '结合掌握度与考试节点生成周计划与每日任务清单。', 'STUDENT', 'Calendar', 'deepseek-chat', '/ai/marketplace/v05/tool_learning_plan', 'V05_NOTICE', '计划,学生', 0, 0, 540, 1),
('tool_ppt', 'AI PPT 生成', '根据大纲快速生成课件骨架与讲稿要点', '输出章节页结构与演讲备注，辅助课件制作。', 'TEACHER', 'Monitor', 'deepseek-chat', '/ai/marketplace/v05/tool_ppt', 'V05_NOTICE', 'PPT,教师', 0, 0, 260, 1),
('tool_polish', 'AI 教学文本润色', '优化题干表述，消除歧义与语病', '面向试题、教案与通知类文本提供学术化润色建议。', 'GENERAL', 'EditPen', 'deepseek-chat', '/ai/marketplace/v05/tool_polish', 'V05_NOTICE', '润色,通用', 0, 0, 380, 1),
('tool_translate', 'AI 双语专业翻译', '中英计算机与专业课术语精准对照翻译', '保持术语一致性，适合双语课件与论文摘要翻译。', 'GENERAL', 'Connection', 'deepseek-chat', '/ai/marketplace/v05/tool_translate', 'V05_NOTICE', '翻译,通用', 0, 0, 450, 1),
('tool_resource_rec', 'AI 资源推荐', '基于课程与章节推荐关联课件与视频资源', '在学习中心展示与当前课程强关联的习题与教学资源。', 'STUDENT', 'Promotion', 'deepseek-chat', '/learning/recommendations', 'ROUTE', '推荐,学生', 1, 0, 1320, 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  detailed_intro = VALUES(detailed_intro),
  category = VALUES(category),
  icon = VALUES(icon),
  model_id = VALUES(model_id),
  route = VALUES(route),
  execution_mode = VALUES(execution_mode),
  tags = VALUES(tags),
  is_recommended = VALUES(is_recommended),
  is_hot = VALUES(is_hot),
  use_count = VALUES(use_count),
  status = VALUES(status);
