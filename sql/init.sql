-- =============================================================================
-- 智教云 · EduMind 数据库全量初始化脚本
-- 文件：sql/init.sql
-- 说明：包含全量 43 张表结构定义 + 丰富完整的核心业务种子数据（含 V0.5 RAG/Chunk/Prompt 治理 + V1.0 学情/图谱/Gateway/Agent）
-- 适配：MySQL 8.0+ / utf8mb4 / MyBatis-Plus Java Entity 100% 对齐
-- 执行：mysql -u root -p < sql/init.sql
-- 默认账号：admin / teacher / student / student2，密码均为 admin123
-- =============================================================================

CREATE DATABASE IF NOT EXISTS edumind
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE edumind;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 一、系统管理（用户 / 角色 / 权限 / 通知）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user_preference;
DROP TABLE IF EXISTS sys_ai_quota;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_notification;
DROP TABLE IF EXISTS sys_config;

CREATE TABLE sys_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username     VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录名',
    password     VARCHAR(128) NOT NULL COMMENT '密码（BCrypt密文）',
    real_name    VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    email        VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone        VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    avatar       VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    status       VARCHAR(16)  DEFAULT 'ENABLE' COMMENT 'ENABLE/DISABLE',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code   VARCHAR(32)  NOT NULL UNIQUE COMMENT '角色标识（ADMIN/TEACHER/STUDENT）',
    role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE sys_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(64)  NOT NULL UNIQUE COMMENT '权限标识',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    parent_id       BIGINT       DEFAULT 0 COMMENT '父级权限ID',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

CREATE TABLE sys_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE sys_notification (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    user_id     BIGINT       NOT NULL COMMENT '接收用户ID',
    title       VARCHAR(128) NOT NULL COMMENT '通知标题',
    content     TEXT         DEFAULT NULL COMMENT '通知正文',
    type        VARCHAR(32)  DEFAULT 'SYSTEM' COMMENT '通知类型（SYSTEM/COURSE/EXAM/ASSIGNMENT）',
    is_read     TINYINT      DEFAULT 0 COMMENT '是否已读（0-未读 1-已读）',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息通知表';

CREATE TABLE sys_config (
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

-- -----------------------------------------------------------------------------
-- 二、课程教学（课程 / 章节 / 知识点 / 选课成员）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS course_member;
DROP TABLE IF EXISTS course_knowledge_point;
DROP TABLE IF EXISTS course_chapter;
DROP TABLE IF EXISTS course;

CREATE TABLE course (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课程ID',
    title       VARCHAR(128) NOT NULL COMMENT '课程名称',
    code        VARCHAR(64)  DEFAULT NULL COMMENT '课程编码',
    teacher_id  BIGINT       DEFAULT NULL COMMENT '责任教师用户ID',
    semester    VARCHAR(32)  DEFAULT NULL COMMENT '开课学期',
    description TEXT         DEFAULT NULL COMMENT '课程简介',
    cover_image VARCHAR(512) DEFAULT NULL COMMENT '封面图URL',
    status      INT          DEFAULT 1 COMMENT '状态（1-正常 0-归档）',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

CREATE TABLE course_chapter (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '章节ID',
    course_id   BIGINT       NOT NULL COMMENT '所属课程ID',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父章节ID',
    title       VARCHAR(128) NOT NULL COMMENT '章节标题',
    sort_order  INT          DEFAULT 0 COMMENT '排序号',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程章节表';

CREATE TABLE course_knowledge_point (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识点ID',
    course_id   BIGINT       NOT NULL COMMENT '所属课程ID',
    chapter_id  BIGINT       DEFAULT NULL COMMENT '所属章节ID',
    title       VARCHAR(128) NOT NULL COMMENT '知识点名称',
    sort_order  INT          DEFAULT 0 COMMENT '排序号',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id),
    KEY idx_chapter_id (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程知识点表';

CREATE TABLE course_member (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    course_id   BIGINT      NOT NULL COMMENT '课程ID',
    user_id     BIGINT      NOT NULL COMMENT '用户ID',
    member_role VARCHAR(16) NOT NULL COMMENT '角色（TEACHER/STUDENT）',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_user (course_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程成员表';

-- -----------------------------------------------------------------------------
-- 三、题库与试卷（题库 / 试题 / 选项 / 试卷 / 组卷关联）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS exam_question;
DROP TABLE IF EXISTS teaching_exam;
DROP TABLE IF EXISTS question_bank_item;
DROP TABLE IF EXISTS question_option;
DROP TABLE IF EXISTS edu_question;
DROP TABLE IF EXISTS question_bank;

CREATE TABLE question_bank (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题库ID',
    name           VARCHAR(128) NOT NULL COMMENT '题库名称',
    course_id      BIGINT       DEFAULT NULL COMMENT '所属课程ID',
    description    TEXT         DEFAULT NULL COMMENT '题库描述',
    question_count INT          DEFAULT 0 COMMENT '题目总数',
    status         INT          DEFAULT 1 COMMENT '状态（1-正常 0-停用）',
    deleted        TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';

CREATE TABLE edu_question (
    id                 BIGINT       NOT NULL COMMENT '题目ID（支持分布式雪花ID）',
    bank_id            BIGINT       DEFAULT NULL COMMENT '所属题库ID',
    course_id          BIGINT       DEFAULT NULL COMMENT '所属课程ID',
    knowledge_point_id BIGINT       DEFAULT NULL COMMENT '关联知识点ID',
    stem               TEXT         NOT NULL COMMENT '题干内容',
    type               VARCHAR(32)  NOT NULL COMMENT '题型（SINGLE_CHOICE/MULTIPLE_CHOICE/JUDGMENT/BLANK/ESSAY）',
    options            TEXT         DEFAULT NULL COMMENT '选项列表（JSON格式）',
    answer             TEXT         DEFAULT NULL COMMENT '参考答案',
    analysis           TEXT         DEFAULT NULL COMMENT '解析与解题思路',
    difficulty         INT          DEFAULT 3 COMMENT '难度系数（1-5）',
    score              INT          DEFAULT 5 COMMENT '默认分值',
    status             INT          DEFAULT 1 COMMENT '状态（1-正常 0-停用）',
    deleted            TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_bank_id (bank_id),
    KEY idx_course_id (course_id),
    KEY idx_kp_id (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试题表';

CREATE TABLE question_option (
    id          BIGINT     NOT NULL AUTO_INCREMENT COMMENT '选项ID',
    question_id BIGINT     NOT NULL COMMENT '所属试题ID',
    option_key  VARCHAR(8) DEFAULT NULL COMMENT '选项标识（A/B/C/D）',
    content     TEXT       DEFAULT NULL COMMENT '选项文本内容',
    is_correct  TINYINT    DEFAULT 0 COMMENT '是否为正确答案（1-是 0-否）',
    PRIMARY KEY (id),
    KEY idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试题选项明细表';

CREATE TABLE question_bank_item (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    bank_id     BIGINT   NOT NULL COMMENT '题库ID',
    question_id BIGINT   NOT NULL COMMENT '试题ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bank_question (bank_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库试题关联表';

CREATE TABLE teaching_exam (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
    course_id        BIGINT       NOT NULL COMMENT '所属课程ID',
    title            VARCHAR(128) NOT NULL COMMENT '试卷标题',
    total_score      INT          DEFAULT 100 COMMENT '试卷满分',
    pass_score       INT          DEFAULT 60 COMMENT '及格分',
    duration_minutes INT          DEFAULT 90 COMMENT '考试时长（分钟）',
    start_time       DATETIME     DEFAULT NULL COMMENT '开始时间',
    end_time         DATETIME     DEFAULT NULL COMMENT '结束时间',
    status           INT          DEFAULT 1 COMMENT '状态（0-草稿 1-已发布 2-已结束）',
    deleted          TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷测验表';

CREATE TABLE exam_question (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    exam_id     BIGINT NOT NULL COMMENT '试卷ID',
    question_id BIGINT NOT NULL COMMENT '试题ID',
    score       INT    DEFAULT 5 COMMENT '本题在试卷中的分值',
    sort_order  INT    DEFAULT 0 COMMENT '试卷内题目序号',
    PRIMARY KEY (id),
    KEY idx_exam_id (exam_id),
    KEY idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷题目关联表';

-- -----------------------------------------------------------------------------
-- 四、作业与批改（作业 / 提交 / 作答 / 智能批改结果）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS grading_result;
DROP TABLE IF EXISTS submission_answer;
DROP TABLE IF EXISTS assignment_submission;
DROP TABLE IF EXISTS assignment;

CREATE TABLE assignment (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '作业ID',
    course_id   BIGINT       NOT NULL COMMENT '所属课程ID',
    exam_id     BIGINT       DEFAULT NULL COMMENT '关联试卷测验ID（可选）',
    title       VARCHAR(128) NOT NULL COMMENT '作业标题',
    description TEXT         DEFAULT NULL COMMENT '作业说明与要求',
    deadline    DATETIME     DEFAULT NULL COMMENT '截止提交时间',
    status      VARCHAR(16)  DEFAULT 'DRAFT' COMMENT '状态（DRAFT/PUBLISHED/CLOSED）',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学作业表';

CREATE TABLE assignment_submission (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '提交记录ID',
    assignment_id BIGINT      NOT NULL COMMENT '作业ID',
    student_id    BIGINT      NOT NULL COMMENT '提交学生用户ID',
    status        VARCHAR(16) DEFAULT 'IN_PROGRESS' COMMENT '状态（IN_PROGRESS/SUBMITTED/GRADED）',
    total_score   INT         DEFAULT NULL COMMENT '最终得分',
    max_score     INT         DEFAULT NULL COMMENT '满分值',
    submit_time   DATETIME    DEFAULT NULL COMMENT '提交时间',
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assignment_student (assignment_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生作业提交记录表';

CREATE TABLE submission_answer (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '答题记录ID',
    submission_id BIGINT   NOT NULL COMMENT '作业提交记录ID',
    question_id   BIGINT   NOT NULL COMMENT '题目ID',
    answer        TEXT     DEFAULT NULL COMMENT '学生作答内容',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_submission_id (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生答题详情表';

CREATE TABLE grading_result (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '批改结果ID',
    submission_id   BIGINT      NOT NULL COMMENT '作业提交记录ID',
    question_id     BIGINT      NOT NULL COMMENT '题目ID',
    score           INT         DEFAULT 0 COMMENT '得分',
    max_score       INT         DEFAULT 0 COMMENT '本题满分',
    is_correct      TINYINT     DEFAULT NULL COMMENT '判定（1-正确 0-错误 2-部分正确）',
    ai_comment      TEXT        DEFAULT NULL COMMENT 'AI智能批改分析与建议',
    teacher_comment TEXT        DEFAULT NULL COMMENT '教师补充评语',
    status          VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态（PENDING/AI_GRADED/CONFIRMED）',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_submission_id (submission_id),
    KEY idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业批改判定表';

-- -----------------------------------------------------------------------------
-- 五、知识库管理（知识库 / 文档 / 文本切片）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS knowledge_chunk_index;
DROP TABLE IF EXISTS knowledge_index_task;
DROP TABLE IF EXISTS knowledge_document_chunk;
DROP TABLE IF EXISTS knowledge_document_text;
DROP TABLE IF EXISTS knowledge_document;
DROP TABLE IF EXISTS knowledge_base;

CREATE TABLE knowledge_base (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    name           VARCHAR(128) NOT NULL COMMENT '知识库名称',
    course_id      BIGINT       DEFAULT NULL COMMENT '关联课程ID',
    description    TEXT         DEFAULT NULL COMMENT '知识库描述',
    document_count INT          DEFAULT 0 COMMENT '收录文档总数',
    chunk_count    INT          DEFAULT 0 COMMENT 'Chunk 总数',
    index_status   VARCHAR(32)  DEFAULT 'PENDING' COMMENT '向量索引状态',
    status         INT          DEFAULT 1 COMMENT '状态（1-启用 0-禁用）',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程知识库表';

CREATE TABLE knowledge_document (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    knowledge_base_id BIGINT       NOT NULL COMMENT '所属知识库ID',
    file_name         VARCHAR(256) NOT NULL COMMENT '文件名称',
    file_type         VARCHAR(32)  DEFAULT NULL COMMENT '文件类型（PDF/WORD/MD/TXT）',
    file_size         BIGINT       DEFAULT 0 COMMENT '文件字节大小',
    object_key        VARCHAR(512) DEFAULT NULL COMMENT 'MinIO/OSS对象存储路径',
    parse_status      VARCHAR(16)  DEFAULT 'PENDING' COMMENT '解析状态（PENDING/PARSING/SUCCESS/FAILED）',
    error_message     TEXT         DEFAULT NULL COMMENT '错误信息',
    status            INT          DEFAULT 1 COMMENT '状态（1-正常 0-禁用）',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_kb_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

CREATE TABLE knowledge_document_text (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '切片文本ID',
    document_id BIGINT   NOT NULL UNIQUE COMMENT '所属文档ID',
    content     LONGTEXT DEFAULT NULL COMMENT '切片提取正文/纯文本内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doc_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库切片纯文本表';

CREATE TABLE knowledge_document_chunk (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Chunk ID',
    document_id       BIGINT       NOT NULL COMMENT '所属文档ID',
    knowledge_base_id BIGINT       NOT NULL COMMENT '所属知识库ID',
    chunk_index       INT          NOT NULL DEFAULT 0 COMMENT '切片序号',
    content           TEXT         NOT NULL COMMENT '切片正文',
    page_no           INT          DEFAULT NULL COMMENT '页码',
    heading           VARCHAR(256) DEFAULT NULL COMMENT '章节标题',
    char_count        INT          DEFAULT 0 COMMENT '字符数',
    token_estimate    INT          DEFAULT 0 COMMENT 'Token 估算',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_document_id (document_id),
    KEY idx_kb_id (knowledge_base_id),
    KEY idx_doc_chunk_index (document_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档切片表';

CREATE TABLE knowledge_index_task (
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

CREATE TABLE knowledge_chunk_index (
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
-- 六、教学资源（资源 / 课程资源关联）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS course_resource;
DROP TABLE IF EXISTS teaching_resource;

CREATE TABLE teaching_resource (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资源ID',
    course_id     BIGINT       DEFAULT NULL COMMENT '所属课程ID',
    chapter_id    BIGINT       DEFAULT NULL COMMENT '所属章节ID',
    title         VARCHAR(128) NOT NULL COMMENT '资源标题',
    resource_type VARCHAR(32)  DEFAULT NULL COMMENT '类型（PDF/WORD/PPT/VIDEO）',
    file_url      VARCHAR(512) DEFAULT NULL COMMENT '资源访问URL',
    description   TEXT         DEFAULT NULL COMMENT '资源介绍',
    status        INT          DEFAULT 1 COMMENT '状态（1-正常 0-下架）',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id),
    KEY idx_chapter_id (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学资源表';

CREATE TABLE course_resource (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    course_id     BIGINT       NOT NULL COMMENT '课程ID',
    resource_id   BIGINT       DEFAULT NULL COMMENT '教学资源ID',
    document_id   BIGINT       DEFAULT NULL COMMENT '知识库文档ID',
    title         VARCHAR(128) DEFAULT NULL COMMENT '资源名称快照',
    resource_type VARCHAR(32)  DEFAULT NULL COMMENT '资源类型快照',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程资源关联表';

-- -----------------------------------------------------------------------------
-- 七、AI 智能体系（AI 工具 / 对话会话 / 消息记录 / 模型调用日志）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS prompt_template_version;
DROP TABLE IF EXISTS prompt_template;
DROP TABLE IF EXISTS ai_message;
DROP TABLE IF EXISTS ai_conversation;
DROP TABLE IF EXISTS ai_call_log;
DROP TABLE IF EXISTS ai_tool;

CREATE TABLE ai_tool (
    id             VARCHAR(64)  NOT NULL COMMENT '工具唯一标识',
    name           VARCHAR(128) NOT NULL COMMENT '工具名称',
    description    VARCHAR(512) DEFAULT NULL COMMENT '工具描述',
    detailed_intro VARCHAR(1024) DEFAULT NULL COMMENT '详情页长描述',
    category       VARCHAR(32)  DEFAULT NULL COMMENT '适用对象（TEACHER/STUDENT/GENERAL）',
    icon           VARCHAR(64)  DEFAULT NULL COMMENT '图标名',
    model_id       VARCHAR(64)  DEFAULT NULL COMMENT '默认模型',
    route          VARCHAR(256) DEFAULT NULL COMMENT '前端导航路由',
    execution_mode VARCHAR(16)  DEFAULT 'ROUTE' COMMENT 'ROUTE|V05_NOTICE',
    tags           VARCHAR(256) DEFAULT NULL COMMENT '搜索标签（逗号分隔）',
    is_recommended TINYINT      DEFAULT 0 COMMENT '是否精选推荐（1-是 0-否）',
    is_hot         TINYINT      DEFAULT 0 COMMENT '热门标记（1-是 0-否）',
    use_count      INT          DEFAULT 0 COMMENT '累计调用热度',
    status         INT          DEFAULT 1 COMMENT '状态（1-可用 0-下架）',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI工具广场元数据表';

CREATE TABLE ai_conversation (
    id            VARCHAR(64)  NOT NULL COMMENT '会话UUID',
    user_id       BIGINT       NOT NULL COMMENT '发起用户ID',
    course_id     BIGINT       DEFAULT NULL COMMENT '关联课程ID（可选）',
    title         VARCHAR(128) DEFAULT NULL COMMENT '会话标题',
    message_count INT          DEFAULT 0 COMMENT '消息总数',
    total_tokens  INT          DEFAULT 0 COMMENT '消耗Token总量',
    deleted       TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

CREATE TABLE ai_message (
    id              VARCHAR(64) NOT NULL COMMENT '消息UUID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '所属会话ID',
    role            VARCHAR(16) NOT NULL COMMENT '发送方角色（user/assistant/system）',
    content         TEXT        DEFAULT NULL COMMENT '消息文本',
    citations_json  TEXT        DEFAULT NULL COMMENT '引用 JSON（RAG 溯源）',
    token_count     INT         DEFAULT 0 COMMENT '本次Token消耗',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话消息记录表';

CREATE TABLE ai_call_log (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id             BIGINT      DEFAULT NULL COMMENT '调用用户ID',
    model               VARCHAR(64) DEFAULT NULL COMMENT '调用的LLM模型名',
    prompt_tokens       INT         DEFAULT 0 COMMENT 'Prompt Token数',
    completion_tokens   INT         DEFAULT 0 COMMENT 'Completion Token数',
    latency_ms          INT         DEFAULT 0 COMMENT '响应耗时（毫秒）',
    scene               VARCHAR(64) DEFAULT NULL COMMENT '调用业务场景',
    knowledge_base_id   BIGINT      DEFAULT NULL COMMENT '知识库ID（RAG）',
    retrieval_hit_count INT         DEFAULT 0 COMMENT '检索命中数',
    citation_doc_ids    VARCHAR(512) DEFAULT NULL COMMENT '引用文档ID列表',
    create_time         DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型调用日志表';

CREATE TABLE prompt_template (
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

CREATE TABLE prompt_template_version (
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

CREATE TABLE sys_ai_quota (
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

CREATE TABLE sys_user_preference (
    user_id            BIGINT       NOT NULL COMMENT '用户ID',
    theme              VARCHAR(16)  DEFAULT 'LIGHT' COMMENT '主题',
    language           VARCHAR(16)  DEFAULT 'zh-CN' COMMENT '语言',
    default_model      VARCHAR(64)  DEFAULT NULL COMMENT '默认模型',
    enable_rag         TINYINT(1)   DEFAULT 1 COMMENT '默认启用 RAG',
    enable_notification TINYINT(1)  DEFAULT 1 COMMENT '启用通知',
    preferences_json   JSON         DEFAULT NULL COMMENT '扩展偏好 JSON',
    updated_at         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置';

-- -----------------------------------------------------------------------------
-- 八、统计分析（每日学情与 AI 消耗统计快照）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS statistics_daily_snapshot;

CREATE TABLE statistics_daily_snapshot (
    id                     BIGINT NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    stat_date              DATE   NOT NULL COMMENT '快照日期',
    course_id              BIGINT DEFAULT NULL COMMENT '课程ID（NULL表示全局全校汇总）',
    active_student_count   INT    DEFAULT 0 COMMENT '当日活跃学生数',
    total_ai_conversations INT    DEFAULT 0 COMMENT '当日AI对话总轮次',
    total_tokens_consumed  BIGINT DEFAULT 0 COMMENT '当日Token消耗总量',
    avg_score              DOUBLE DEFAULT NULL COMMENT '当日作业/测验平均分',
    PRIMARY KEY (id),
    KEY idx_stat_date (stat_date),
    KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日学情与AI统计快照表';

-- -----------------------------------------------------------------------------
-- 九、V1.0 智能教学中枢（学情 / 图谱关系 / AI Gateway / Agent）
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS agent_tool_call;
DROP TABLE IF EXISTS agent_step;
DROP TABLE IF EXISTS agent_run;
DROP TABLE IF EXISTS ai_gateway_route;
DROP TABLE IF EXISTS ai_model_config;
DROP TABLE IF EXISTS knowledge_point_relation;
DROP TABLE IF EXISTS course_statistics;
DROP TABLE IF EXISTS wrong_question_record;
DROP TABLE IF EXISTS knowledge_mastery;
DROP TABLE IF EXISTS learning_record;

CREATE TABLE learning_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    student_id      BIGINT       NOT NULL COMMENT '学生ID',
    course_id       BIGINT       NOT NULL COMMENT '课程ID',
    action_type     VARCHAR(32)  NOT NULL COMMENT 'LOGIN/STUDY/RESOURCE_VIEW/AI_CHAT',
    duration_minutes INT         DEFAULT 0 COMMENT '学习时长（分钟）',
    resource_id     BIGINT       DEFAULT NULL COMMENT '关联资源ID',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_student_course (student_id, course_id),
    KEY idx_course_time (course_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习行为明细';

CREATE TABLE knowledge_mastery (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '掌握度ID',
    student_id          BIGINT        NOT NULL COMMENT '学生ID',
    course_id           BIGINT        NOT NULL COMMENT '课程ID',
    knowledge_point_id  BIGINT        NOT NULL COMMENT '知识点ID',
    mastery_score       DECIMAL(5,4)  NOT NULL DEFAULT 0 COMMENT '掌握度得分 0~1',
    sample_count        INT           NOT NULL DEFAULT 0 COMMENT '评估样本数',
    last_assessed_at    DATETIME      DEFAULT NULL COMMENT '最近评估时间',
    create_time         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_kp (student_id, knowledge_point_id),
    KEY idx_course_kp (course_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点掌握度';

CREATE TABLE wrong_question_record (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '错题记录ID',
    student_id          BIGINT       NOT NULL COMMENT '学生ID',
    course_id           BIGINT       NOT NULL COMMENT '课程ID',
    question_id         BIGINT       NOT NULL COMMENT '题目ID',
    knowledge_point_id  BIGINT       DEFAULT NULL COMMENT '关联知识点ID',
    error_types         VARCHAR(128) DEFAULT NULL COMMENT 'CONCEPT,LOGIC,CALC',
    diagnosis           VARCHAR(512) DEFAULT NULL COMMENT '错因诊断',
    variant_question_ids VARCHAR(256) DEFAULT NULL COMMENT '变式题ID列表',
    wrong_count         INT          NOT NULL DEFAULT 1 COMMENT '累计错误次数',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_course_question (course_id, question_id),
    KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题记录';

CREATE TABLE course_statistics (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '统计ID',
    course_id       BIGINT        NOT NULL COMMENT '课程ID',
    stat_date       DATE          NOT NULL COMMENT '统计日期',
    active_users    INT           DEFAULT 0 COMMENT '活跃用户数',
    avg_score       DECIMAL(5,2)  DEFAULT NULL COMMENT '平均分',
    completion_rate DECIMAL(5,4)  DEFAULT NULL COMMENT '完成率',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_date (course_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程日聚合';

CREATE TABLE knowledge_point_relation (
    id                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '关系ID',
    source_knowledge_point_id BIGINT       NOT NULL COMMENT '源知识点ID',
    target_knowledge_point_id BIGINT       NOT NULL COMMENT '目标知识点ID',
    relation_type           VARCHAR(32)  NOT NULL COMMENT 'prerequisite/successor/related/assessed_by/supported_by',
    properties              JSON         DEFAULT NULL COMMENT '扩展属性',
    create_time             DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_relation (source_knowledge_point_id, target_knowledge_point_id, relation_type),
    KEY idx_source (source_knowledge_point_id),
    KEY idx_target (target_knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点关系边';

CREATE TABLE ai_model_config (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    model_key           VARCHAR(64)  NOT NULL COMMENT '模型标识',
    provider            VARCHAR(32)  NOT NULL COMMENT '提供商',
    enabled             TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    priority            INT          NOT NULL DEFAULT 1 COMMENT '路由优先级',
    fallback_model_key  VARCHAR(64)  DEFAULT NULL COMMENT '降级模型',
    max_tokens          INT          DEFAULT 4096 COMMENT '最大 Token',
    temperature         DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_key (model_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型配置';

CREATE TABLE ai_gateway_route (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '路由ID',
    scene               VARCHAR(32)  NOT NULL COMMENT 'CHAT/RAG/AGENT/GRADING',
    primary_model_key   VARCHAR(64)  NOT NULL COMMENT '主模型',
    fallback_model_key  VARCHAR(64)  DEFAULT NULL COMMENT '降级模型',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_scene (scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Gateway 场景路由';

CREATE TABLE agent_run (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    run_id          VARCHAR(64)  NOT NULL COMMENT '运行实例ID',
    agent_code      VARCHAR(32)  NOT NULL COMMENT 'Agent 编码',
    user_id         BIGINT       NOT NULL COMMENT '发起用户ID',
    course_id       BIGINT       DEFAULT NULL COMMENT '关联课程ID',
    goal            TEXT         NOT NULL COMMENT '执行目标',
    status          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING' COMMENT '运行状态',
    result_json     JSON         DEFAULT NULL COMMENT '结果 JSON',
    model_key       VARCHAR(64)  DEFAULT NULL COMMENT '使用模型',
    token_usage     INT          DEFAULT 0 COMMENT 'Token 消耗',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_id (run_id),
    KEY idx_user (user_id),
    KEY idx_agent (agent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 执行实例';

CREATE TABLE agent_step (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
    run_id          VARCHAR(64)  NOT NULL COMMENT '运行实例ID',
    step_index      INT          NOT NULL COMMENT '步骤序号',
    step_type       VARCHAR(16)  NOT NULL COMMENT 'INTENT/PLAN/TOOL/LLM/RESULT',
    title           VARCHAR(256) NOT NULL COMMENT '步骤标题',
    tool_name       VARCHAR(64)  DEFAULT NULL COMMENT '工具名称',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '步骤状态',
    input_preview   TEXT         DEFAULT NULL COMMENT '输入预览',
    output_preview  TEXT         DEFAULT NULL COMMENT '输出预览',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_run (run_id, step_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 步骤';

CREATE TABLE agent_tool_call (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '调用ID',
    run_id          VARCHAR(64)  NOT NULL COMMENT '运行实例ID',
    step_id         BIGINT       DEFAULT NULL COMMENT '关联步骤ID',
    tool_name       VARCHAR(64)  NOT NULL COMMENT '工具名称',
    input_json      JSON         DEFAULT NULL COMMENT '输入 JSON',
    output_json     JSON         DEFAULT NULL COMMENT '输出 JSON',
    status          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING' COMMENT '调用状态',
    duration_ms     INT          DEFAULT 0 COMMENT '耗时（毫秒）',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_run_tool (run_id, tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tool 调用日志';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 全量业务种子数据初始化（真实匹配各模块页面渲染需求）
-- =============================================================================

-- 1. 系统角色
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN',   '系统管理员', '平台系统全量管理权限'),
(2, 'TEACHER', '教师',       '教学管理、出题组卷与作业批改'),
(3, 'STUDENT', '学生',       '课程学习、在线测试与智能练习');

-- 2. 系统用户（密码统一为 admin123）
INSERT INTO sys_user (id, username, password, real_name, email, phone, avatar, status) VALUES
(1, 'admin',     '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', '系统管理员', 'admin@edumind.edu',     '13800000001', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(2, 'teacher',   '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', '张老师',     'teacher@edumind.edu',   '13800000002', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(3, 'student',   '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', '李同学',     'student@edumind.edu',   '13800000003', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(4, 'student2',  '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', '王同学',     'student2@edumind.edu',  '13800000004', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE');

-- 3. 用户与角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 3);

-- 4. 细粒度权限
INSERT INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(1,  'system:user:view',    '用户查看', 0),
(2,  'system:user:edit',    '用户编辑', 0),
(3,  'system:role:view',    '角色查看', 0),
(4,  'system:role:edit',    '角色编辑', 0),
(5,  'course:view',         '课程查看', 0),
(6,  'course:create',       '课程创建', 0),
(7,  'course:edit',         '课程编辑', 0),
(8,  'question:view',       '题目查看', 0),
(9,  'question:edit',       '题目编辑', 0),
(10, 'exam:view',           '试卷查看', 0),
(11, 'exam:edit',           '试卷编辑', 0),
(12, 'assignment:view',     '作业查看', 0),
(13, 'assignment:create',   '作业创建', 0),
(14, 'assignment:grade',    '作业批改', 0),
(15, 'knowledge:view',      '知识库查看', 0),
(16, 'knowledge:edit',      '知识库编辑', 0),
(17, 'ai:chat',             'AI对话', 0),
(18, 'ai:grading',          'AI批改', 0),
(19, 'ai:question',         'AI出题', 0),
(20, 'ai:exam',             'AI组卷', 0),
(21, 'analytics:view',      '学情分析查看', 0),
(22, 'resource:view',       '资源查看', 0),
(23, 'resource:upload',     '资源上传', 0),
(24, 'notice:view',         '通知查看', 0),
(25, 'knowledge:rag:debug',   'RAG调试', 0),
(26, 'system:prompt:view',    'Prompt查看', 0),
(27, 'system:prompt:edit',    'Prompt编辑', 0),
(28, 'system:audit:view',     'AI审计查看', 0),
(29, 'system:quota:view',     'AI配额查看', 0),
(30, 'system:quota:edit',     'AI配额编辑', 0),
(31, 'ai:tool:use',           'Agent工具调用', 0);

-- 管理员全量权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 教师教学权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code NOT LIKE 'system:%';

-- 学生端学习权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE permission_code IN (
    'course:view', 'assignment:view', 'exam:view', 'knowledge:view', 'ai:chat', 'resource:view', 'notice:view'
);

-- 5. 系统全局配置
INSERT INTO sys_config (config_key, config_value, config_name, config_group, remark) VALUES
('sys.mail.config', '{"enabled":false,"host":"smtp.qq.com","port":465,"username":"","password":"","fromName":"智教云 · EduMind","useSsl":true,"codeExpireMinutes":5,"codeIntervalSeconds":60,"dailyLimitPerEmail":10}', '邮件发送服务配置(SMTP)', 'mail', 'SMTP发信参数、发信人名称与验证码防刷策略'),
('sys.base.info', '{"platformName":"智教云 · EduMind","subTitle":"AI 智能教学赋能平台","copyright":"© 2026 EduMind. All rights reserved.","icp":"京ICP备20260001号-1"}', '平台基础信息配置', 'base', '平台站点标题、副标与备案声明');

-- 6. Prompt 模板（RAG 对话默认模板）
INSERT INTO prompt_template (code, name, category, status, version, content, variables) VALUES
('chat_rag', '课程AI-RAG对话', 'rag', 'PUBLISHED', 1,
 '你是课程 AI 助教。请基于以下资料回答用户问题。\n\n【参考资料】\n{{context}}\n\n【用户问题】\n{{question}}',
 'context,question');

-- 7. 系统通知
INSERT INTO sys_notification (id, user_id, title, content, type, is_read, create_time) VALUES
(1, 3, '【作业截止提醒】第一单元链表作业即将截止', '您选修的《数据结构与算法》课程第一单元作业截止时间为今晚 23:59，请及时完成并提交作答。', 'ASSIGNMENT', 0, NOW()),
(2, 3, '【AI批改完成】单链表设计作业已完成评分', '张老师已确认您的作业批改成绩，综合得分 92 分，点击可查看详细 AI 知识盲点诊断与教师评语。', 'ASSIGNMENT', 1, NOW()),
(3, 3, '【系统升级】AI 智能助教与知识库升级通知', '平台已上线基于 RAG 的课程知识库向量问答增强系统，欢迎在课程详情中向 AI 助教提问！', 'SYSTEM', 0, NOW()),
(4, 2, '【批改待办】有 2 份学生作业待教师复核', '《数据结构与算法》课程中有 2 名学生已提交作业，AI 辅助批改已初步打分，请您查阅。', 'COURSE', 0, NOW()),
(5, 2, '【知识库提示】文档向量切片解析成功', '您上传的《数据结构第二章-线性表与链表深度解析.pdf》已完成切片与向量索引构建，已可供智能出题调用。', 'SYSTEM', 1, NOW());

-- 6. 课程
INSERT INTO course (id, title, code, teacher_id, semester, description, cover_image, status) VALUES
(101, '数据结构与算法',     'CS201',   2, '2025秋', '计算机核心专业课，涵盖线性表、栈、队列、二叉树、图结构及常见排序检索算法设计。', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500', 1),
(102, 'Java面向对象程序设计', 'CS101',   2, '2025秋', '面向对象高级特性、集合框架、多线程并发与企业级工程架构实战。',       'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500', 1),
(103, '高等数学（上）',     'MATH101', 2, '2025秋', '大学理工科公共核心基础，重点讲解极限论、导数与微分、不定积分与定积分应用。',   'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=500', 1);

-- 7. 章节
INSERT INTO course_chapter (id, course_id, parent_id, title, sort_order) VALUES
(1,  101, 0,  '第一章 绪论与算法分析',      1),
(2,  101, 1,  '1.1 算法复杂度与渐进表示法',  1),
(3,  101, 0,  '第二章 线性表结构',          2),
(4,  101, 3,  '2.1 顺序存储与实现',          1),
(5,  101, 3,  '2.2 链式存储与单双向链表',    2),
(6,  101, 0,  '第三章 树与二叉树',          3),
(7,  102, 0,  '第一章 Java入门与基础语法',  1),
(8,  102, 7,  '1.1 变量作用域与数据类型',    1),
(9,  102, 0,  '第二章 面向对象核心思想',    2),
(10, 102, 9,  '2.1 封装、继承与多态机制',    1),
(11, 103, 0,  '第一章 函数与极限论',        1),
(12, 103, 11, '1.1 数列与函数极限计算',      1),
(13, 103, 0,  '第二章 导数与微分',          2),
(14, 103, 13, '2.1 复合函数与隐函数求导',    1);

-- 8. 知识点
INSERT INTO course_knowledge_point (id, course_id, chapter_id, title, sort_order) VALUES
(10, 101, 2,  '时间与空间复杂度分析',            1),
(11, 101, 4,  '顺序表插入与删除时间开销',        2),
(12, 101, 5,  '单链表就地逆置算法',              3),
(13, 101, 6,  '二叉树先序中序后序遍历',          4),
(14, 102, 8,  '基本数据类型与包装类自动拆装箱',  1),
(15, 102, 10, '面向对象三大特征与多态运行时绑定', 2),
(16, 102, 10, 'ArrayList 与 LinkedList 源码剖析', 3),
(17, 103, 12, '等价无穷小代换及其应用条件',      1),
(18, 103, 12, '洛必达法则求未定式极限',          2),
(19, 103, 14, '复合函数链式求导法则',            3);

-- 9. 课程选课成员
INSERT INTO course_member (course_id, user_id, member_role) VALUES
(101, 2, 'TEACHER'), (101, 3, 'STUDENT'), (101, 4, 'STUDENT'),
(102, 2, 'TEACHER'), (102, 3, 'STUDENT'), (102, 4, 'STUDENT'),
(103, 2, 'TEACHER'), (103, 3, 'STUDENT'), (103, 4, 'STUDENT');

-- 10. 题库
INSERT INTO question_bank (id, name, course_id, description, question_count, status, deleted) VALUES
(1, '数据结构核心真题库',   101, '涵盖全国统考408与期末高频真题，包括线性表、树与排序算法', 5, 1, 0),
(2, 'Java面向对象精选题集', 102, 'Java基础语法、面向对象、集合框架与异常处理典型题型',     3, 1, 0),
(3, '高等数学期末测试真题库', 103, '极限、连续、导数与微积分计算经典测试题',                 3, 1, 0);

-- 11. 试题明细
INSERT INTO edu_question (id, bank_id, course_id, knowledge_point_id, stem, type, options, answer, analysis, difficulty, score, status, deleted) VALUES
(1001, 3, 103, 17, '当 $x \\to 0$ 时，下列无穷小量中与 $x$ 等价的无穷小量是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"$\\\\sin 2x$"},{"key":"B","content":"$\\\\ln(1 + x)$"},{"key":"C","content":"$1 - \\\\cos x$"},{"key":"D","content":"$e^x - 1 - x$"}]',
 'B', '根据等价无穷小基本公式，当 $x \\to 0$ 时，$\\\\ln(1+x) \\sim x$；而 $\\\\sin 2x \\sim 2x$，$1-\\\\cos x \\sim \\\\frac{1}{2}x^2$。故正确答案为 B。', 3, 5, 1, 0),

(1002, 3, 103, 19, '设函数 $f(x) = \\ln(1 + x^2)$，则导数 $f\'(1)$ 的值为（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"$\\\\frac{1}{2}$"},{"key":"B","content":"$1$"},{"key":"C","content":"$2$"},{"key":"D","content":"$\\\\ln 2$"}]',
 'B', '求复合函数导数：$f\'(x) = \\\\frac{1}{1 + x^2} \\\\cdot 2x = \\\\frac{2x}{1 + x^2}$，代入 $x = 1$ 得 $f\'(1) = \\\\frac{2}{2} = 1$。故选 B。', 2, 5, 1, 0),

(1003, 1, 101, 10, '已知一个栈的入栈序列为 1, 2, 3, 4, 5，则不可能得到的出栈序列是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"$4, 5, 3, 2, 1$"},{"key":"B","content":"$4, 3, 5, 1, 2$"},{"key":"C","content":"$1, 5, 4, 2, 3$"},{"key":"D","content":"$3, 4, 2, 1, 5$"}]',
 'B', '选项B中，当4、3出栈后，栈内剩余1、2，后压入5出栈后，栈顶应为2，不可能先出1再出2。故出栈序列 $4, 3, 5, 1, 2$ 不合法。', 3, 5, 1, 0),

(1004, 1, 101, 13, '下列关于平衡二叉树（AVL树）的叙述中，正确的有（ ）。', 'MULTIPLE_CHOICE',
 '[{"key":"A","content":"任意结点的左、右子树高度差绝对值不超过1"},{"key":"B","content":"查找操作的时间复杂度在最坏情况下为 $O(\\\\log n)$"},{"key":"C","content":"插入新结点引发失衡后，至多需要两次单旋转即可恢复平衡"},{"key":"D","content":"完全二叉树一定是AVL树"}]',
 'AB', '选项A为AVL树定义；选项B时间复杂度为对数级，正确；完全二叉树不一定是平衡查找树。故正确答案为 AB。', 4, 6, 1, 0),

(1005, 1, 101, 12, '在单链表中，增加头结点的目的是为了在首元结点之前插入新结点和删除首元结点的操作与其它结点的操作统一。', 'JUDGMENT',
 '[{"key":"T","content":"正确"},{"key":"F","content":"错误"}]',
 'T', '头结点的引入使得对首元结点的操作与后续结点的操作相同，无需单独维护头指针变量的重定向，统一了边界处理。', 2, 3, 1, 0),

(1006, 1, 101, 10, '请简要描述快速排序（QuickSort）的核心分治思想，并分析其最好、平均与最坏情况下的时间复杂度。', 'ESSAY',
 '[]',
 '分治思想：1. 选取基准元素pivot；2. 分区划分将小于等于pivot的放左侧，大于的放右侧；3. 递归排序左右两部分。时间复杂度：最好和平均均为O(nlogn)，最坏O(n^2)。',
 '考查快速排序的分治划分机制以及分区不平衡导致的退化现象。', 3, 10, 1, 0),

(1007, 2, 102, 16, '在 Java 集合框架中，关于 ArrayList 与 LinkedList 的特性描述，正确的是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"ArrayList 底层是动态数组，随机访问时间复杂度为 $O(1)$"},{"key":"B","content":"LinkedList 支持基于下标的 $O(1)$ 常数时间随机访问"},{"key":"C","content":"ArrayList 插入元素永远不需要复制数组"},{"key":"D","content":"LinkedList 占用内存比 ArrayList 更少"}]',
 'A', 'ArrayList底层是Object[]数组，支持下标随机访问；LinkedList为双向链表，查找需要遍历，且包含前后节点引用指针额外开销。故正确答案为 A。', 2, 5, 1, 0),

(1008, 2, 102, 14, 'Java 语言中，所有类的最终根父类是 ______。', 'BLANK',
 '[]',
 'java.lang.Object',
 'Java中任何未显式指定父类的类都隐式继承自 java.lang.Object。', 1, 4, 1, 0);

-- 12. 试题选项
INSERT INTO question_option (id, question_id, option_key, content, is_correct) VALUES
(1,  1001, 'A', '$\\sin 2x$', 0),
(2,  1001, 'B', '$\\ln(1 + x)$', 1),
(3,  1001, 'C', '$1 - \\cos x$', 0),
(4,  1001, 'D', '$e^x - 1 - x$', 0),
(5,  1002, 'A', '$\\frac{1}{2}$', 0),
(6,  1002, 'B', '$1$', 1),
(7,  1002, 'C', '$2$', 0),
(8,  1002, 'D', '$\\ln 2$', 0),
(9,  1003, 'A', '$4, 5, 3, 2, 1$', 0),
(10, 1003, 'B', '$4, 3, 5, 1, 2$', 1),
(11, 1003, 'C', '$1, 5, 4, 2, 3$', 0),
(12, 1003, 'D', '$3, 4, 2, 1, 5$', 0),
(13, 1004, 'A', '任意结点的左、右子树高度差绝对值不超过1', 1),
(14, 1004, 'B', '查找操作的时间复杂度在最坏情况下为 $O(\\log n)$', 1),
(15, 1004, 'C', '插入新结点引发失衡后，至多需要两次单旋转即可恢复平衡', 0),
(16, 1004, 'D', '完全二叉树一定是AVL树', 0),
(17, 1005, 'T', '正确', 1),
(18, 1005, 'F', '错误', 0),
(19, 1007, 'A', 'ArrayList 底层是动态数组，随机访问时间复杂度为 $O(1)$', 1),
(20, 1007, 'B', 'LinkedList 支持基于下标的 $O(1)$ 常数时间随机访问', 0),
(21, 1007, 'C', 'ArrayList 插入元素永远不需要复制数组', 0),
(22, 1007, 'D', 'LinkedList 占用内存比 ArrayList 更少', 0);

-- 13. 题库试题关联
INSERT INTO question_bank_item (bank_id, question_id) VALUES
(1, 1003), (1, 1004), (1, 1005), (1, 1006),
(2, 1007), (2, 1008),
(3, 1001), (3, 1002);

-- 14. 试卷
INSERT INTO teaching_exam (id, course_id, title, total_score, pass_score, duration_minutes, start_time, end_time, status, deleted) VALUES
(501, 103, '2025秋季学期高等数学期中统一水平测试卷', 100, 60, 90,  '2025-10-15 09:00:00', '2025-10-15 10:30:00', 1, 0),
(502, 101, '数据结构与算法分析阶段性上机诊断试卷', 100, 60, 100, '2025-10-20 14:00:00', '2025-10-20 15:40:00', 1, 0);

-- 15. 试卷试题关联
INSERT INTO exam_question (exam_id, question_id, score, sort_order) VALUES
(501, 1001, 10, 1),
(501, 1002, 10, 2),
(502, 1003, 10, 1),
(502, 1004, 15, 2),
(502, 1005, 10, 3),
(502, 1006, 25, 4);

-- 16. 作业任务
INSERT INTO assignment (id, course_id, exam_id, title, description, deadline, status) VALUES
(201, 101, NULL, '第一单元：线性表与链表编程作业', '请完成单链表的基本操作及逆置算法设计，按要求提交核心复杂度分析。', '2025-10-25 23:59:59', 'PUBLISHED'),
(202, 103, 501,  '高数第三周同步随堂小测',       '函数极限计算与等价无穷小代换小测验，共2道题，限时45分钟。',         '2025-10-18 23:59:59', 'PUBLISHED');

-- 17. 学生作业提交
INSERT INTO assignment_submission (id, assignment_id, student_id, status, total_score, max_score, submit_time) VALUES
(301, 201, 3, 'GRADED',    92, 100, '2025-10-20 16:30:00'),
(302, 201, 4, 'SUBMITTED', NULL, 100, '2025-10-21 11:15:00');

-- 18. 学生作答记录
INSERT INTO submission_answer (id, submission_id, question_id, answer) VALUES
(401, 301, 1003, 'B'),
(402, 301, 1005, 'T'),
(403, 301, 1006, '快排基于分治思想：1. 选取基准值(pivot)；2. 将小于等于pivot的放左侧，大于的放右侧；3. 递归排序左右两部分。最好和平均时间复杂度为O(nlogn)，最坏情况当已有序时退化为O(n^2)。');

-- 19. 批改结果
INSERT INTO grading_result (id, submission_id, question_id, score, max_score, is_correct, ai_comment, teacher_comment, status) VALUES
(501, 301, 1003, 5,  5,  1, '作答完全正确，清晰理解了栈后进先出的约束条件。', '优秀', 'CONFIRMED'),
(502, 301, 1005, 3,  3,  1, '回答正确，头结点统一了空表和非空表的插入删除逻辑。', '完全正确', 'CONFIRMED'),
(503, 301, 1006, 9, 10,  2, '【AI批改分析】分治三个阶段描述准确，时间复杂度分析完备。失分点：未提及三数取中等优化避免退化的工程实践。', '思路很清晰，继续保持！', 'CONFIRMED');

-- 20. 知识库
INSERT INTO knowledge_base (id, name, course_id, description, document_count, status) VALUES
(1, '数据结构与算法专业知识库', 101, '包含数据结构核心讲义、经典算法图解、大厂高频面试真题解析与课后作业参考。', 2, 1),
(2, 'Java面向对象程序设计知识库', 102, 'Java基础概念、JVM内存模型、多线程并发与常见框架最佳实践。', 1, 1);

-- 21. 知识库文档
INSERT INTO knowledge_document (id, knowledge_base_id, file_name, file_type, file_size, object_key, parse_status, status) VALUES
(1, 1, '数据结构第二章-线性表与链表深度解析.pdf', 'PDF',  2457600, 'kb/1/cs201_ch2.pdf',        'SUCCESS', 1),
(2, 1, '常见经典树与图算法图解.pdf',             'PDF',  4194304, 'kb/1/cs201_tree_graph.pdf', 'SUCCESS', 1),
(3, 2, 'Java面向对象编程实战教程.docx',           'WORD', 1572864, 'kb/2/java_oop.docx',        'SUCCESS', 1);

-- 22. 知识库切片纯文本
INSERT INTO knowledge_document_text (id, document_id, content) VALUES
(1, 1, '【线性表定义与特征】线性表是具有相同数据类型的n(n>=0)个数据元素的有限序列。其存储结构分为顺序存储与链式存储。顺序表物理地址连续，具备O(1)随机访问能力；链表通过指针域链接节点，适合频繁插入与删除。单链表头结点能统一首元结点与中间结点的操作边界。'),
(2, 2, '【二叉树核心性质与平衡树】非空二叉树上叶子结点数等于度为2的结点数加1。二叉平衡树(AVL)任何结点的左右子树高度差绝对值不超过1。当插入新结点引发失衡时，根据插入路径分为LL、RR、LR、RL四种形态，分别通过右旋、左旋或双旋在常数时间内恢复平衡。'),
(3, 3, '【面向对象三大特性剖析】封装隐藏了对象的内部细节，对外提供安全受控的公共访问入口；继承实现了代码复用与类型扩展；多态使得统一接口可以根据运行时的实际对象类型呈现不同的行为。多态三大必要条件：继承、方法重写、父类引用指向子类对象。');

-- 23. 教学资源
INSERT INTO teaching_resource (id, course_id, chapter_id, title, resource_type, file_url, description, status) VALUES
(1, 101, 1, '数据结构课件-第一章绪论.pdf',       'PDF',  '/resources/cs201-ch1.pdf',  '第一章绪论课件与教学目标', 1),
(2, 101, 3, '线性表算法设计与实验指导书.docx',   'WORD', '/resources/cs201-lab2.docx', '实验二线性表上机实践指导', 1),
(3, 102, 7, 'Java快速入门与JDK环境搭建指南.pptx', 'PPT',  '/resources/java-intro.pptx', '第一讲基础入门幻灯片',     1);

-- 24. 课程资源绑定
INSERT INTO course_resource (id, course_id, resource_id, document_id, title, resource_type) VALUES
(1, 101, 1, NULL, '数据结构课件-第一章绪论.pdf',       'PDF'),
(2, 101, 2, NULL, '线性表算法设计与实验指导书.docx',   'WORD'),
(3, 101, NULL, 1, '数据结构第二章-线性表与链表深度解析.pdf', 'PDF'),
(4, 102, 3, NULL, 'Java快速入门与JDK环境搭建指南.pptx', 'PPT');

-- 25. AI 工具广场（14 项）
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
('tool_resource_rec', 'AI 资源推荐', '基于课程与章节推荐关联课件与视频资源', '在学习中心展示与当前课程强关联的习题与教学资源。', 'STUDENT', 'Promotion', 'deepseek-chat', '/learning/recommendations', 'ROUTE', '推荐,学生', 1, 0, 1320, 1);

-- 26. AI 示例会话
INSERT INTO ai_conversation (id, user_id, course_id, title, message_count, total_tokens, deleted) VALUES
('conv_teacher_001', 2, 101, '数据结构期中试卷出题咨询', 2, 850, 0),
('conv_student_001', 3, 101, '关于单链表逆置算法的疑问解答', 2, 620, 0);

-- 27. AI 会话消息
INSERT INTO ai_message (id, conversation_id, role, content, token_count) VALUES
('msg_t_01', 'conv_teacher_001', 'user', '请根据数据结构第二章线性表知识点，帮我生成3道考察链表边界处理的单选题，难度中等。', 80),
('msg_t_02', 'conv_teacher_001', 'assistant', '好的张老师！已为您基于《数据结构》第二章生成 3 道考察链表边界指针处理的选择题，已自动匹配知识点【单链表就地逆置】与【顺序表边界条件】，请前往试题库查看或一键导入组卷。', 770),
('msg_s_01', 'conv_student_001', 'user', '助教你好，请问在做单链表就地逆置时，为什么必须先用临时指针保存 next 节点？', 60),
('msg_s_02', 'conv_student_001', 'assistant', '同学你好！在单链表就地逆置时，当我们执行 `curr->next = prev;` 反转当前节点的指针指向后，原有的后续节点链条就会断开。如果不提前使用 `temp = curr->next;` 记录后续节点地址，将无法继续遍历剩余链表，造成链表丢失（内存泄漏或无法循环）。', 560);

-- 28. AI 调用日志
INSERT INTO ai_call_log (id, user_id, model, prompt_tokens, completion_tokens, latency_ms, scene) VALUES
(1, 2, 'deepseek-chat', 320, 530, 1250, 'QUESTION_GEN'),
(2, 3, 'deepseek-chat', 180, 440, 890,  'AI_CHAT');

-- 29. 每日学情与 AI 消耗统计快照（近 7 天连续趋势，供大屏与仪表盘直接渲染）
INSERT INTO statistics_daily_snapshot (id, stat_date, course_id, active_student_count, total_ai_conversations, total_tokens_consumed, avg_score) VALUES
(1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 101, 45, 120, 85000,  84.5),
(2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 101, 52, 145, 98000,  85.0),
(3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 101, 58, 160, 112000, 86.2),
(4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 101, 64, 188, 135000, 87.1),
(5, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 101, 70, 210, 158000, 86.8),
(6, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 101, 78, 235, 182000, 88.0),
(7, CURDATE(),                           101, 85, 260, 205000, 88.5);

-- 30. AI Gateway 默认模型与场景路由（V1.0）
INSERT INTO ai_model_config (model_key, provider, enabled, priority, fallback_model_key, max_tokens, temperature) VALUES
('deepseek-chat', 'deepseek', 1, 1, 'mock', 4096, 0.70),
('qwen-turbo', 'qwen', 1, 2, 'mock', 4096, 0.70),
('mock', 'mock', 1, 99, NULL, 4096, 0.70);

INSERT INTO ai_gateway_route (scene, primary_model_key, fallback_model_key) VALUES
('CHAT', 'deepseek-chat', 'mock'),
('RAG', 'deepseek-chat', 'mock'),
('AGENT', 'deepseek-chat', 'mock'),
('GRADING', 'deepseek-chat', 'mock');

-- 31. V1.0 学情与掌握度样本（课程 102 Java，供学情分析与 Gate G 演示）
INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at) VALUES
(3, 102, 14, 0.8500, 5, NOW()),
(3, 102, 15, 0.6400, 4, NOW()),
(3, 102, 16, 0.7800, 3, NOW()),
(4, 102, 14, 0.8200, 4, NOW()),
(4, 102, 15, 0.7000, 3, NOW()),
(4, 102, 16, 0.7500, 3, NOW());

INSERT INTO knowledge_point_relation (source_knowledge_point_id, target_knowledge_point_id, relation_type) VALUES
(15, 14, 'prerequisite');

INSERT INTO wrong_question_record (student_id, course_id, question_id, knowledge_point_id, error_types, diagnosis, wrong_count) VALUES
(3, 102, 1007, 16, 'CONCEPT,LOGIC', '混淆编译期与运行期绑定', 3);

INSERT INTO learning_record (student_id, course_id, action_type, duration_minutes) VALUES
(3, 102, 'STUDY', 45),
(4, 102, 'STUDY', 60);

-- =============================================================================
-- 初始化完成：包含全量 43 张业务表结构与完整种子数据
-- =============================================================================
