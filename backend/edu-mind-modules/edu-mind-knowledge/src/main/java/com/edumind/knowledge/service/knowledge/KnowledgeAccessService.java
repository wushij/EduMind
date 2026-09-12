package com.edumind.knowledge.service.knowledge;

import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeAccessService {

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final CourseQueryApi courseQueryApi;

    public KnowledgeBaseEntity assertAccessible(Long knowledgeBaseId) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseDao.findById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在");
        }
        assertCourseAccessible(knowledgeBase.getCourseId());
        return knowledgeBase;
    }

    public void assertCourseAccessible(Long courseId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        if (isAdmin()) {
            return;
        }
        if (courseId == null) {
            return;
        }
        if (!courseQueryApi.isCourseMember(courseId, userId)) {
            throw new BusinessException("无权访问该知识库");
        }
    }

    public List<Long> listAccessibleCourseIds() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        if (isAdmin()) {
            return knowledgeBaseDao.findAll().stream()
                    .map(KnowledgeBaseEntity::getCourseId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return courseQueryApi.listCourseIdsByUserId(userId);
    }

    public List<KnowledgeBaseEntity> listAccessibleKnowledgeBases(Long courseId) {
        if (courseId != null) {
            assertCourseAccessible(courseId);
            return knowledgeBaseDao.findByCourseId(courseId);
        }
        List<Long> courseIds = listAccessibleCourseIds();
        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return knowledgeBaseDao.findByCourseIds(courseIds);
    }

    public void assertDocumentAccessible(Long documentId, Long knowledgeBaseId) {
        assertAccessible(knowledgeBaseId);
    }

    private boolean isAdmin() {
        LoginUser user = UserContext.get();
        return user != null
                && user.getRoles() != null
                && user.getRoles().contains(SecurityConstant.ROLE_ADMIN);
    }
}
