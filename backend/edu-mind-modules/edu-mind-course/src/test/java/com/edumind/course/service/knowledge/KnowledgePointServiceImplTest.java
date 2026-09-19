package com.edumind.course.service.knowledge;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.KnowledgePointDao;
import com.edumind.course.dto.knowledge.KnowledgePointCreateDTO;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.KnowledgePointEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.knowledge.impl.KnowledgePointServiceImpl;
import com.edumind.knowledge.api.KnowledgePointRelationCommandApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgePointServiceImplTest {

    @Mock
    private CourseDao courseDao;
    @Mock
    private ChapterDao chapterDao;
    @Mock
    private KnowledgePointDao knowledgePointDao;
    @Mock
    private CourseConverter courseConverter;
    @Mock
    private CourseAccessService courseAccessService;
    @Mock
    private KnowledgePointRelationCommandApi knowledgePointRelationCommandApi;

    @InjectMocks
    private KnowledgePointServiceImpl knowledgePointService;

    @Test
    void create_persistsFullFieldsAndSyncsPrerequisites() {
        CourseEntity course = new CourseEntity();
        course.setId(101L);
        when(courseDao.findById(101L)).thenReturn(course);

        ChapterEntity chapter = new ChapterEntity();
        chapter.setId(2L);
        chapter.setCourseId(101L);
        when(chapterDao.findById(2L)).thenReturn(chapter);
        when(knowledgePointDao.findByCourseId(101L, null)).thenReturn(List.of());
        when(knowledgePointDao.countByCourseId(101L)).thenReturn(5L);
        when(knowledgePointRelationCommandApi.listPrerequisiteTargetsBySourceIds(any())).thenReturn(Map.of());

        KnowledgePointCreateDTO dto = new KnowledgePointCreateDTO();
        dto.setChapterId(2L);
        dto.setTitle("main 方法规范");
        dto.setDescription("掌握入口与签名");
        dto.setCognitiveDimension("APPLY");
        dto.setImportance(4);
        dto.setExamFocus("签名拼写错误");
        dto.setPrerequisiteIds(List.of());

        when(knowledgePointDao.insert(any())).thenAnswer(inv -> {
            KnowledgePointEntity e = inv.getArgument(0);
            e.setId(120L);
            return 1;
        });
        when(courseConverter.toKnowledgePointVO(any())).thenReturn(
                com.edumind.course.vo.knowledge.KnowledgePointVO.builder().id(120L).title("main 方法规范").build());

        knowledgePointService.create(101L, dto);

        ArgumentCaptor<KnowledgePointEntity> captor = ArgumentCaptor.forClass(KnowledgePointEntity.class);
        verify(knowledgePointDao).insert(captor.capture());
        KnowledgePointEntity saved = captor.getValue();
        assertEquals("main 方法规范", saved.getTitle());
        assertEquals("掌握入口与签名", saved.getDescription());
        assertEquals("APPLY", saved.getCognitiveDimension());
        assertEquals(4, saved.getImportance());
        assertEquals("签名拼写错误", saved.getExamFocus());
        assertEquals("KP-101-006", saved.getCode());
        verify(knowledgePointRelationCommandApi).syncPrerequisites(120L, List.of());
    }

    @Test
    void create_rejectsChapterFromOtherCourse() {
        CourseEntity course = new CourseEntity();
        course.setId(101L);
        when(courseDao.findById(101L)).thenReturn(course);

        ChapterEntity chapter = new ChapterEntity();
        chapter.setId(9L);
        chapter.setCourseId(999L);
        when(chapterDao.findById(9L)).thenReturn(chapter);

        KnowledgePointCreateDTO dto = new KnowledgePointCreateDTO();
        dto.setChapterId(9L);
        dto.setTitle("无效章节");

        assertThrows(BusinessException.class, () -> knowledgePointService.create(101L, dto));
    }
}
