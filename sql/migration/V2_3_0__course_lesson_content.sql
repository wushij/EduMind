-- ==========================================================
-- V2.3.0: micro-lesson content, lesson-KP link, lesson progress
-- Idempotent: safe to re-run on partially migrated databases.
-- Windows / Navicat: run whole file in one go (UTF-8).
-- ==========================================================

DROP PROCEDURE IF EXISTS edumind_patch_v230_lesson;

DELIMITER //
CREATE PROCEDURE edumind_patch_v230_lesson()
BEGIN
    DECLARE db_name VARCHAR(64);
    SET db_name = DATABASE();

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_chapter' AND column_name = 'description'
    ) THEN
        ALTER TABLE course_chapter ADD COLUMN description TEXT NULL AFTER title;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_chapter' AND column_name = 'duration_minutes'
    ) THEN
        ALTER TABLE course_chapter ADD COLUMN duration_minutes INT NULL AFTER sort_order;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_chapter' AND column_name = 'lesson_type'
    ) THEN
        ALTER TABLE course_chapter ADD COLUMN lesson_type VARCHAR(16) NULL AFTER duration_minutes;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_chapter' AND column_name = 'content_json'
    ) THEN
        ALTER TABLE course_chapter ADD COLUMN content_json MEDIUMTEXT NULL AFTER lesson_type;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_chapter' AND column_name = 'content_status'
    ) THEN
        ALTER TABLE course_chapter ADD COLUMN content_status VARCHAR(16) DEFAULT 'DRAFT' AFTER content_json;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_chapter' AND column_name = 'published_at'
    ) THEN
        ALTER TABLE course_chapter ADD COLUMN published_at DATETIME NULL AFTER content_status;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_knowledge_point' AND column_name = 'code'
    ) THEN
        ALTER TABLE course_knowledge_point ADD COLUMN code VARCHAR(32) NULL AFTER title;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_knowledge_point' AND column_name = 'description'
    ) THEN
        ALTER TABLE course_knowledge_point ADD COLUMN description TEXT NULL AFTER code;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_knowledge_point' AND column_name = 'cognitive_dimension'
    ) THEN
        ALTER TABLE course_knowledge_point ADD COLUMN cognitive_dimension VARCHAR(16) NULL AFTER description;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'course_knowledge_point' AND column_name = 'importance'
    ) THEN
        ALTER TABLE course_knowledge_point ADD COLUMN importance TINYINT DEFAULT 3 AFTER cognitive_dimension;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'learning_record' AND column_name = 'chapter_id'
    ) THEN
        ALTER TABLE learning_record ADD COLUMN chapter_id BIGINT NULL AFTER resource_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'learning_record' AND column_name = 'knowledge_point_id'
    ) THEN
        ALTER TABLE learning_record ADD COLUMN knowledge_point_id BIGINT NULL AFTER chapter_id;
    END IF;
END //
DELIMITER ;

CALL edumind_patch_v230_lesson();
DROP PROCEDURE IF EXISTS edumind_patch_v230_lesson;

CREATE TABLE IF NOT EXISTS course_chapter_knowledge_point (
    id                  BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant',
    course_id           BIGINT NOT NULL COMMENT 'course',
    chapter_id          BIGINT NOT NULL COMMENT 'lesson chapter id',
    knowledge_point_id  BIGINT NOT NULL COMMENT 'knowledge point id',
    sort_order          INT DEFAULT 0 COMMENT 'sort',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created',
    KEY idx_tenant_id (tenant_id),
    PRIMARY KEY (id),
    UNIQUE KEY uk_chapter_kp (chapter_id, knowledge_point_id),
    KEY idx_course_chapter (course_id, chapter_id),
    KEY idx_knowledge_point (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='lesson knowledge point link';

CREATE TABLE IF NOT EXISTS course_lesson_progress (
    id                  BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant',
    student_id          BIGINT NOT NULL COMMENT 'student',
    course_id           BIGINT NOT NULL COMMENT 'course',
    lesson_chapter_id   BIGINT NOT NULL COMMENT 'lesson chapter id',
    status              VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED/IN_PROGRESS/COMPLETED',
    progress_percent    INT NOT NULL DEFAULT 0 COMMENT '0-100',
    last_block_id       VARCHAR(64) DEFAULT NULL COMMENT 'last block id',
    last_study_at       DATETIME DEFAULT NULL COMMENT 'last study time',
    completed_at        DATETIME DEFAULT NULL COMMENT 'completed time',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created',
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated',
    KEY idx_tenant_id (tenant_id),
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_lesson (student_id, lesson_chapter_id),
    KEY idx_course_student (course_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='student lesson progress';

-- Demo seed (course 102, lesson chapter 8) -- optional for existing DBs
UPDATE course_chapter SET
    description = '理解 Java 基本数据类型、变量作用域与包装类拆装箱机制。',
    duration_minutes = 30,
    lesson_type = 'LECTURE',
    content_status = 'PUBLISHED',
    published_at = NOW(),
    content_json = '{"version":1,"blocks":[{"type":"callout","variant":"objective","title":"学习目标","body":"掌握基本数据类型分类；理解包装类与自动拆装箱。"},{"type":"markdown","body":"## 核心概念\\n\\nJava 将数据类型分为基本类型与引用类型。基本类型直接存储值，引用类型存储对象地址。"},{"type":"heading","level":2,"text":"实践要点"}]}'
WHERE id = 8 AND course_id = 102;

UPDATE course_knowledge_point SET
    code = 'KP-014',
    description = '八种基本类型与默认值规则',
    cognitive_dimension = 'UNDERSTAND',
    importance = 4
WHERE id = 14;

INSERT IGNORE INTO course_chapter_knowledge_point (tenant_id, course_id, chapter_id, knowledge_point_id, sort_order)
SELECT 1, 102, 8, 14, 1 FROM DUAL
WHERE EXISTS (SELECT 1 FROM course_chapter WHERE id = 8)
  AND NOT EXISTS (
      SELECT 1 FROM course_chapter_knowledge_point
      WHERE chapter_id = 8 AND knowledge_point_id = 14
  );
