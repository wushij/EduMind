package com.edumind.knowledge.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeRagStatsMapper {

    @Select("SELECT COUNT(*) FROM knowledge_document")
    long countDocuments();

    @Select("SELECT COUNT(*) FROM knowledge_document WHERE source_type = 'LESSON'")
    long countLessonDocuments();

    @Select("SELECT COUNT(*) FROM knowledge_document WHERE source_type IS NULL OR source_type = 'UPLOAD'")
    long countUploadDocuments();

    @Select("SELECT COUNT(DISTINCT document_id) FROM knowledge_document_chunk")
    long countIndexedDocuments();

    @Select("SELECT COUNT(*) FROM knowledge_document_chunk")
    long countTotalChunks();

    @Select("""
            SELECT COUNT(*) FROM knowledge_document_chunk c
            INNER JOIN knowledge_document d ON c.document_id = d.id
            WHERE d.source_type = 'LESSON'
            """)
    long countLessonChunks();

    @Select("""
            SELECT COUNT(*) FROM knowledge_document_chunk c
            INNER JOIN knowledge_document d ON c.document_id = d.id
            WHERE d.source_type IS NULL OR d.source_type = 'UPLOAD'
            """)
    long countUploadChunks();

    @Select("SELECT COUNT(*) FROM knowledge_chunk_index WHERE embed_status = 'INDEXED'")
    long countIndexedChunkIndexes();

    @Select("SELECT COUNT(*) FROM knowledge_chunk_index WHERE embed_status = 'FAILED'")
    long countFailedChunkIndexes();

    @Select("""
            SELECT COUNT(*) FROM knowledge_document_chunk c
            LEFT JOIN knowledge_document d ON c.document_id = d.id
            WHERE d.id IS NULL
            """)
    long countOrphanChunks();

    @Select("""
            SELECT c.id FROM knowledge_document_chunk c
            LEFT JOIN knowledge_document d ON c.document_id = d.id
            WHERE d.id IS NULL
            """)
    List<Long> selectOrphanChunkIds();

    @Delete("""
            DELETE c FROM knowledge_document_chunk c
            LEFT JOIN knowledge_document d ON c.document_id = d.id
            WHERE d.id IS NULL
            """)
    int deleteOrphanChunks();

    @Delete("""
            DELETE i FROM knowledge_chunk_index i
            LEFT JOIN knowledge_document_chunk c ON i.chunk_id = c.id
            WHERE c.id IS NULL
            """)
    int deleteOrphanIndexRows();
}
