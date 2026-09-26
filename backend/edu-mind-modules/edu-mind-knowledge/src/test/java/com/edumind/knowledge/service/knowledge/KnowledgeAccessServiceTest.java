package com.edumind.knowledge.service.knowledge;

import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeAccessServiceTest {

    @Mock
    private KnowledgeBaseDao knowledgeBaseDao;

    @Mock
    private CourseQueryApi courseQueryApi;

    @InjectMocks
    private KnowledgeAccessService knowledgeAccessService;

    @BeforeEach
    void setUp() {
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("教师访问未加入的课程知识库应被拒绝")
    void teacherCannotAccessUnenrolledCourseKnowledgeBase() {
        Long teacherId = 10L;
        Long courseId = 999L;
        Long kbId = 1L;

        UserContext.set(LoginUser.builder()
                .id(teacherId)
                .username("teacher1")
                .roles(List.of(SecurityConstant.ROLE_TEACHER))
                .build());

        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setId(kbId);
        kb.setCourseId(courseId);

        when(knowledgeBaseDao.findById(kbId)).thenReturn(kb);
        when(courseQueryApi.isCourseMember(courseId, teacherId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> knowledgeAccessService.assertAccessible(kbId));
        assertTrue(ex.getMessage().contains("无权访问"));
    }

    @Test
    @DisplayName("教师访问已教授或加入的课程知识库应放行")
    void teacherCanAccessEnrolledCourseKnowledgeBase() {
        Long teacherId = 10L;
        Long courseId = 200L;
        Long kbId = 2L;

        UserContext.set(LoginUser.builder()
                .id(teacherId)
                .username("teacher1")
                .roles(List.of(SecurityConstant.ROLE_TEACHER))
                .build());

        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setId(kbId);
        kb.setCourseId(courseId);

        when(knowledgeBaseDao.findById(kbId)).thenReturn(kb);
        when(courseQueryApi.isCourseMember(courseId, teacherId)).thenReturn(true);

        KnowledgeBaseEntity result = knowledgeAccessService.assertAccessible(kbId);
        assertNotNull(result);
        assertEquals(kbId, result.getId());
    }

    @Test
    @DisplayName("教师查询知识库列表只返回自己课程的知识库，不返回全量知识库")
    void teacherListKnowledgeBasesOnlyReturnsAccessibleCourses() {
        Long teacherId = 10L;
        UserContext.set(LoginUser.builder()
                .id(teacherId)
                .username("teacher1")
                .roles(List.of(SecurityConstant.ROLE_TEACHER))
                .build());

        when(courseQueryApi.listCourseIdsByUserId(teacherId)).thenReturn(List.of(200L));

        KnowledgeBaseEntity myKb = new KnowledgeBaseEntity();
        myKb.setId(2L);
        myKb.setCourseId(200L);
        when(knowledgeBaseDao.findByCourseIds(List.of(200L))).thenReturn(List.of(myKb));

        List<KnowledgeBaseEntity> list = knowledgeAccessService.listAccessibleKnowledgeBases(null);

        assertEquals(1, list.size());
        assertEquals(2L, list.get(0).getId());
        verify(knowledgeBaseDao, never()).findAll();
    }

    @Test
    @DisplayName("平台管理员可以查看全量知识库")
    void adminCanAccessAllKnowledgeBases() {
        Long adminId = 1L;
        UserContext.set(LoginUser.builder()
                .id(adminId)
                .username("admin")
                .roles(List.of(SecurityConstant.ROLE_ADMIN))
                .build());

        KnowledgeBaseEntity kb1 = new KnowledgeBaseEntity();
        kb1.setId(1L);
        KnowledgeBaseEntity kb2 = new KnowledgeBaseEntity();
        kb2.setId(2L);

        when(knowledgeBaseDao.findAll()).thenReturn(List.of(kb1, kb2));

        List<KnowledgeBaseEntity> list = knowledgeAccessService.listAccessibleKnowledgeBases(null);
        assertEquals(2, list.size());
    }
}
