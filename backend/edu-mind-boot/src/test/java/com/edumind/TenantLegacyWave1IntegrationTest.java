package com.edumind;

import com.edumind.common.context.TenantContext;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.mapper.ChapterMapper;
import com.edumind.course.mapper.CourseMapper;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.mapper.KnowledgeBaseMapper;
import com.edumind.knowledge.mapper.KnowledgeDocumentMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * GA Wave1 · 高泄漏风险子表跨租户 IDOR 防护集成测试
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TenantLegacyWave1IntegrationTest {

    @Autowired
    private CourseDao courseDao;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ChapterDao chapterDao;
    @Autowired
    private ChapterMapper chapterMapper;
    @Autowired
    private KnowledgeBaseDao knowledgeBaseDao;
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Autowired
    private KnowledgeDocumentDao knowledgeDocumentDao;
    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @BeforeEach
    @AfterEach
    void cleanupContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("[Wave1] course_chapter 跨租户 IDOR 防护")
    void testCourseChapterCrossTenantIsolation() {
        TenantContext.setTenantId(2001L);
        CourseEntity courseA = buildCourse("GA_W1_A");
        courseMapper.insert(courseA);

        TenantContext.setTenantId(2002L);
        CourseEntity courseB = buildCourse("GA_W1_B");
        courseMapper.insert(courseB);

        TenantContext.setTenantId(2001L);
        ChapterEntity chapterA = new ChapterEntity();
        chapterA.setCourseId(courseA.getId());
        chapterA.setParentId(0L);
        chapterA.setTitle("A校第一章");
        chapterA.setSortOrder(1);
        chapterMapper.insert(chapterA);

        TenantContext.setTenantId(2002L);
        ChapterEntity chapterB = new ChapterEntity();
        chapterB.setCourseId(courseB.getId());
        chapterB.setParentId(0L);
        chapterB.setTitle("B校第一章");
        chapterB.setSortOrder(1);
        chapterMapper.insert(chapterB);

        try {
            TenantContext.setTenantId(2001L);
            Assertions.assertNotNull(chapterDao.findById(chapterA.getId()));
            Assertions.assertNull(chapterDao.findById(chapterB.getId()), "A校不可读取B校章节");

            TenantContext.setTenantId(2002L);
            Assertions.assertNotNull(chapterDao.findById(chapterB.getId()));
            Assertions.assertNull(chapterDao.findById(chapterA.getId()), "B校不可读取A校章节");

            TenantContext.clear();
            Assertions.assertNull(chapterDao.findById(chapterA.getId()), "Fail-Closed");
        } finally {
            TenantContext.runWithoutTenant(() -> {
                chapterMapper.deleteById(chapterA.getId());
                chapterMapper.deleteById(chapterB.getId());
                courseMapper.deleteById(courseA.getId());
                courseMapper.deleteById(courseB.getId());
            });
        }
    }

    @Test
    @DisplayName("[Wave1] knowledge_document 跨租户 IDOR 防护")
    void testKnowledgeDocumentCrossTenantIsolation() {
        TenantContext.setTenantId(2001L);
        KnowledgeBaseEntity kbA = buildKb("GA_DOC_A");
        knowledgeBaseMapper.insert(kbA);

        TenantContext.setTenantId(2002L);
        KnowledgeBaseEntity kbB = buildKb("GA_DOC_B");
        knowledgeBaseMapper.insert(kbB);

        TenantContext.setTenantId(2001L);
        KnowledgeDocumentEntity docA = buildDoc(kbA.getId(), "a.pdf");
        knowledgeDocumentMapper.insert(docA);

        TenantContext.setTenantId(2002L);
        KnowledgeDocumentEntity docB = buildDoc(kbB.getId(), "b.pdf");
        knowledgeDocumentMapper.insert(docB);

        try {
            TenantContext.setTenantId(2001L);
            Assertions.assertNotNull(knowledgeDocumentDao.findById(docA.getId()));
            Assertions.assertNull(knowledgeDocumentDao.findById(docB.getId()));

            TenantContext.setTenantId(2002L);
            Assertions.assertNotNull(knowledgeDocumentDao.findById(docB.getId()));
            Assertions.assertNull(knowledgeDocumentDao.findById(docA.getId()));
        } finally {
            TenantContext.runWithoutTenant(() -> {
                knowledgeDocumentMapper.deleteById(docA.getId());
                knowledgeDocumentMapper.deleteById(docB.getId());
                knowledgeBaseMapper.deleteById(kbA.getId());
                knowledgeBaseMapper.deleteById(kbB.getId());
            });
        }
    }

    private CourseEntity buildCourse(String codeSuffix) {
        CourseEntity course = new CourseEntity();
        course.setTenantId(TenantContext.getTenantId());
        course.setCode("GA_W1_" + codeSuffix + "_" + System.currentTimeMillis());
        course.setTitle("GA Wave1 " + codeSuffix);
        course.setTeacherId(1L);
        course.setStatus(1);
        return course;
    }

    private KnowledgeBaseEntity buildKb(String name) {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setTenantId(TenantContext.getTenantId());
        kb.setName(name + "_" + System.currentTimeMillis());
        kb.setStatus(1);
        return kb;
    }

    private KnowledgeDocumentEntity buildDoc(Long kbId, String fileName) {
        KnowledgeDocumentEntity doc = new KnowledgeDocumentEntity();
        doc.setKnowledgeBaseId(kbId);
        doc.setFileName(fileName);
        doc.setFileType("PDF");
        doc.setParseStatus("PENDING");
        doc.setStatus(1);
        return doc;
    }
}
