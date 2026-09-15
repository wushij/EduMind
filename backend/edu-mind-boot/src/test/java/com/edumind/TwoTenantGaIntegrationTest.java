package com.edumind;

import com.edumind.common.context.TenantContext;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.mapper.ChapterMapper;
import com.edumind.course.mapper.CourseMapper;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
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
 * Gate GA-3 · 两校 E2E 核心越权探针（tenant 1 vs tenant 2 自动化子集）
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TwoTenantGaIntegrationTest {

    private static final long TENANT_ONE = 1L;
    private static final long TENANT_TWO = 2L;

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
    void reset() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GA-3: tenant2 token 不可访问 tenant1 chapter/document ID")
    void crossTenantIdorProbe() {
        Long chapterT1;
        Long docT1;

        TenantContext.setTenantId(TENANT_ONE);
        CourseEntity c1 = insertCourse("GA_E2E_T1");
        ChapterEntity ch1 = insertChapter(c1.getId(), "T1-Chapter");
        chapterT1 = ch1.getId();
        KnowledgeBaseEntity kb1 = insertKb("GA_E2E_KB_T1");
        KnowledgeDocumentEntity d1 = insertDoc(kb1.getId(), "t1-doc.pdf");
        docT1 = d1.getId();

        TenantContext.setTenantId(TENANT_TWO);
        CourseEntity c2 = insertCourse("GA_E2E_T2");
        insertChapter(c2.getId(), "T2-Chapter");
        KnowledgeBaseEntity kb2 = insertKb("GA_E2E_KB_T2");
        insertDoc(kb2.getId(), "t2-doc.pdf");

        try {
            Assertions.assertNull(chapterDao.findById(chapterT1), "tenant2 读取 tenant1 chapter 必须为空");
            Assertions.assertNull(knowledgeDocumentDao.findById(docT1), "tenant2 读取 tenant1 document 必须为空");

            TenantContext.setTenantId(TENANT_ONE);
            Assertions.assertNotNull(chapterDao.findById(chapterT1));
            Assertions.assertNotNull(knowledgeDocumentDao.findById(docT1));
        } finally {
            TenantContext.runWithoutTenant(() -> {
                chapterMapper.deleteById(chapterT1);
                knowledgeDocumentMapper.deleteById(docT1);
                knowledgeBaseMapper.deleteById(kb1.getId());
                courseMapper.deleteById(c1.getId());
                courseMapper.deleteById(c2.getId());
                knowledgeBaseMapper.deleteById(kb2.getId());
            });
        }
    }

    private CourseEntity insertCourse(String suffix) {
        CourseEntity course = new CourseEntity();
        course.setTenantId(TenantContext.getTenantId());
        course.setCode("GA3_" + suffix + "_" + System.currentTimeMillis());
        course.setTitle("GA E2E " + suffix);
        course.setStatus(1);
        courseMapper.insert(course);
        return course;
    }

    private ChapterEntity insertChapter(Long courseId, String title) {
        ChapterEntity chapter = new ChapterEntity();
        chapter.setCourseId(courseId);
        chapter.setParentId(0L);
        chapter.setTitle(title);
        chapter.setSortOrder(1);
        chapterMapper.insert(chapter);
        return chapter;
    }

    private KnowledgeBaseEntity insertKb(String name) {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setTenantId(TenantContext.getTenantId());
        kb.setName(name + "_" + System.currentTimeMillis());
        kb.setStatus(1);
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    private KnowledgeDocumentEntity insertDoc(Long kbId, String fileName) {
        KnowledgeDocumentEntity doc = new KnowledgeDocumentEntity();
        doc.setKnowledgeBaseId(kbId);
        doc.setFileName(fileName);
        doc.setFileType("PDF");
        doc.setParseStatus("PENDING");
        doc.setStatus(1);
        knowledgeDocumentMapper.insert(doc);
        return doc;
    }
}
