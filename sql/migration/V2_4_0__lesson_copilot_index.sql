-- V2.4.0: lesson virtual documents for copilot RAG (align Code Compass article index)
DROP PROCEDURE IF EXISTS edumind_patch_v240_lesson_index;

DELIMITER //
CREATE PROCEDURE edumind_patch_v240_lesson_index()
BEGIN
    DECLARE db_name VARCHAR(64);
    SET db_name = DATABASE();

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'knowledge_document' AND column_name = 'source_type'
    ) THEN
        ALTER TABLE knowledge_document
            ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD|LESSON' AFTER knowledge_base_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'knowledge_document' AND column_name = 'course_id'
    ) THEN
        ALTER TABLE knowledge_document
            ADD COLUMN course_id BIGINT NULL COMMENT '课程ID(课节虚拟文档)' AFTER source_type;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'knowledge_document' AND column_name = 'lesson_chapter_id'
    ) THEN
        ALTER TABLE knowledge_document
            ADD COLUMN lesson_chapter_id BIGINT NULL COMMENT '微课节章节ID' AFTER course_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'knowledge_document' AND column_name = 'content_hash'
    ) THEN
        ALTER TABLE knowledge_document
            ADD COLUMN content_hash VARCHAR(64) NULL COMMENT '讲义内容哈希(增量索引)' AFTER lesson_chapter_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = db_name AND table_name = 'knowledge_document' AND index_name = 'uk_lesson_chapter_doc'
    ) THEN
        CREATE UNIQUE INDEX uk_lesson_chapter_doc ON knowledge_document (lesson_chapter_id, source_type);
    END IF;
END//
DELIMITER ;

CALL edumind_patch_v240_lesson_index();
DROP PROCEDURE IF EXISTS edumind_patch_v240_lesson_index;
