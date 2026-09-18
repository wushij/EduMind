package com.edumind.knowledge.service.lesson.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseKnowledgeBaseCommandApi;
import com.edumind.course.api.CourseLessonCatalogApi;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.api.LessonQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.lesson.LessonIndexSourceVO;
import com.edumind.course.vo.lesson.PublishedLessonRefVO;
import com.edumind.infrastructure.vector.VectorStore;
import com.edumind.infrastructure.vector.config.MilvusProperties;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeChunkIndexDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeDocumentTextDao;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseCreateDTO;
import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentTextEntity;
import com.edumind.knowledge.service.chunk.ChunkSplitter;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonContentIndexServiceImpl {

    private static final String SOURCE_LESSON = "LESSON";

    private final LessonQueryApi lessonQueryApi;
    private final CourseQueryApi courseQueryApi;
    private final CourseLessonCatalogApi courseLessonCatalogApi;
    private final CourseKnowledgeBaseCommandApi courseKnowledgeBaseCommandApi;
    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final KnowledgeDocumentTextDao knowledgeDocumentTextDao;
    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    private final KnowledgeChunkIndexDao knowledgeChunkIndexDao;
    private final ChunkSplitter chunkSplitter;
    private final IndexingService indexingService;
    private final VectorStore vectorStore;
    private final MilvusProperties milvusProperties;

    @Transactional(rollbackFor = Exception.class)
    public void ingestLesson(Long courseId, Long lessonChapterId) {
        if (courseId == null || lessonChapterId == null) {
            throw new BusinessException("课节索引参数不完整");
        }
        LessonIndexSourceVO source = lessonQueryApi.getLessonIndexSource(courseId, lessonChapterId);
        if (source == null || !source.isPublished()) {
            log.warn("Skip lesson ingest: not published courseId={} lessonId={}", courseId, lessonChapterId);
            return;
        }
        if (!StringUtils.hasText(source.getMarkdown())) {
            log.warn("Skip lesson ingest: empty markdown courseId={} lessonId={}", courseId, lessonChapterId);
            return;
        }
        Long knowledgeBaseId = resolveKnowledgeBaseId(courseId);
        KnowledgeDocumentEntity existing = knowledgeDocumentDao.findLessonDocument(courseId, lessonChapterId);
        if (existing != null && source.getContentHash().equals(existing.getContentHash())) {
            long chunkCount = knowledgeDocumentChunkDao.countByDocumentId(existing.getId());
            if (chunkCount > 0) {
                log.info("Lesson index unchanged courseId={} lessonId={}", courseId, lessonChapterId);
                return;
            }
            log.warn("Lesson document {} has no chunks, rebuilding slices courseId={} lessonId={}",
                    existing.getId(), courseId, lessonChapterId);
            chunkMarkdown(existing, source.getMarkdown());
            indexingService.triggerIndex(knowledgeBaseId, "INCREMENTAL");
            return;
        }
        if (existing != null) {
            purgeDocumentVectorsAndChunks(existing.getId());
        }
        KnowledgeDocumentEntity document = existing != null ? existing : new KnowledgeDocumentEntity();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setSourceType(SOURCE_LESSON);
        document.setCourseId(courseId);
        document.setLessonChapterId(lessonChapterId);
        document.setContentHash(source.getContentHash());
        document.setFileName(source.getTitle() != null ? source.getTitle() : "课节讲义");
        document.setFileType("LESSON");
        document.setFileSize((long) source.getMarkdown().length());
        document.setObjectKey("");
        document.setParseStatus("SUCCESS");
        document.setErrorMessage(null);
        document.setStatus(1);
        if (existing == null) {
            knowledgeDocumentDao.insert(document);
        } else {
            knowledgeDocumentDao.updateById(document);
        }
        upsertDocumentText(document.getId(), source.getMarkdown());
        chunkMarkdown(document, source.getMarkdown());
        indexingService.triggerIndex(knowledgeBaseId, "INCREMENTAL");
        log.info("Lesson indexed courseId={} lessonId={} documentId={}", courseId, lessonChapterId, document.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeLessonIndex(Long courseId, Long lessonChapterId) {
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findLessonDocument(courseId, lessonChapterId);
        if (document == null) {
            return;
        }
        purgeDocumentVectorsAndChunks(document.getId());
        knowledgeDocumentTextDao.deleteByDocumentId(document.getId());
        knowledgeDocumentDao.deleteById(document.getId());
        refreshKbDocCount(document.getKnowledgeBaseId());
    }

    public Optional<Long> findLessonDocumentId(Long courseId, Long lessonChapterId) {
        KnowledgeDocumentEntity document = knowledgeDocumentDao.findLessonDocument(courseId, lessonChapterId);
        return document == null ? Optional.empty() : Optional.of(document.getId());
    }

    public int reindexPublishedLessons(Long courseId) {
        List<PublishedLessonRefVO> refs = courseLessonCatalogApi.listPublishedLessonChapters(courseId);
        int count = 0;
        for (PublishedLessonRefVO ref : refs) {
            try {
                ingestLesson(ref.getCourseId(), ref.getLessonChapterId());
                count++;
            } catch (Exception ex) {
                log.error("Reindex lesson failed courseId={} lessonId={}: {}",
                        ref.getCourseId(), ref.getLessonChapterId(), ex.getMessage());
            }
        }
        return count;
    }

    private Long resolveKnowledgeBaseId(Long courseId) {
        CourseDetailVO course = courseQueryApi.getCourseById(courseId);
        if (course != null && course.getKnowledgeBaseId() != null) {
            return course.getKnowledgeBaseId();
        }
        List<KnowledgeBaseEntity> bases = knowledgeBaseDao.findByCourseId(courseId);
        if (!bases.isEmpty()) {
            return bases.get(0).getId();
        }
        String name = course != null && StringUtils.hasText(course.getName())
                ? course.getName() + "·讲义索引"
                : "课程讲义索引";
        KnowledgeBaseCreateDTO dto = new KnowledgeBaseCreateDTO();
        dto.setName(name);
        dto.setDescription("课节讲义与课程资料检索库");
        dto.setCourseId(courseId);
        Long kbId = knowledgeBaseService.create(dto);
        courseKnowledgeBaseCommandApi.bindKnowledgeBaseId(courseId, kbId);
        return kbId;
    }

    private void upsertDocumentText(Long documentId, String markdown) {
        KnowledgeDocumentTextEntity text = knowledgeDocumentTextDao.findByDocumentId(documentId);
        if (text == null) {
            text = new KnowledgeDocumentTextEntity();
            text.setDocumentId(documentId);
            text.setContent(markdown);
            knowledgeDocumentTextDao.insert(text);
        } else {
            text.setContent(markdown);
            knowledgeDocumentTextDao.updateById(text);
        }
    }

    private void chunkMarkdown(KnowledgeDocumentEntity document, String markdown) {
        List<ChunkSplitter.SplitChunk> splits = chunkSplitter.split(markdown);
        for (ChunkSplitter.SplitChunk split : splits) {
            KnowledgeDocumentChunkEntity chunk = new KnowledgeDocumentChunkEntity();
            chunk.setDocumentId(document.getId());
            chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunk.setChunkIndex(split.getChunkIndex());
            chunk.setContent(split.getContent());
            chunk.setPageNo(split.getPageNo());
            chunk.setHeading(split.getHeading());
            chunk.setCharCount(split.getCharCount());
            chunk.setTokenEstimate(split.getTokenEstimate());
            knowledgeDocumentChunkDao.insert(chunk);
        }
        refreshKbDocCount(document.getKnowledgeBaseId());
    }

    private void purgeDocumentVectorsAndChunks(Long documentId) {
        List<KnowledgeDocumentChunkEntity> chunks = knowledgeDocumentChunkDao.findByDocumentId(documentId);
        String collection = milvusProperties.getCollection();
        for (KnowledgeDocumentChunkEntity chunk : chunks) {
            try {
                vectorStore.delete(collection, String.valueOf(chunk.getId()));
            } catch (Exception ex) {
                log.warn("Vector delete failed chunkId={}: {}", chunk.getId(), ex.getMessage());
            }
        }
        knowledgeChunkIndexDao.deleteByDocumentId(documentId);
        knowledgeDocumentChunkDao.deleteByDocumentId(documentId);
    }

    private void refreshKbDocCount(Long knowledgeBaseId) {
        KnowledgeBaseEntity kb = knowledgeBaseDao.findById(knowledgeBaseId);
        if (kb == null) {
            return;
        }
        kb.setDocCount((int) knowledgeDocumentDao.countByKnowledgeBaseId(knowledgeBaseId));
        kb.setChunkCount((int) knowledgeDocumentChunkDao.countByKnowledgeBaseId(knowledgeBaseId));
        knowledgeBaseDao.updateById(kb);
    }
}
