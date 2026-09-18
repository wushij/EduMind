-- V2.5.0: persist embedding vectors in MySQL for dev reload (Milvus 关闭时启动恢复)
DROP PROCEDURE IF EXISTS edumind_patch_v250_chunk_embedding;

DELIMITER //
CREATE PROCEDURE edumind_patch_v250_chunk_embedding()
BEGIN
    DECLARE db_name VARCHAR(64);
    SET db_name = DATABASE();

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = db_name AND table_name = 'knowledge_chunk_index' AND column_name = 'embedding_vector'
    ) THEN
        ALTER TABLE knowledge_chunk_index
            ADD COLUMN embedding_vector MEDIUMTEXT NULL COMMENT 'Embedding JSON 数组(内存向量库恢复)' AFTER embedding_model;
    END IF;
END //
DELIMITER ;

CALL edumind_patch_v250_chunk_embedding();
DROP PROCEDURE IF EXISTS edumind_patch_v250_chunk_embedding;
