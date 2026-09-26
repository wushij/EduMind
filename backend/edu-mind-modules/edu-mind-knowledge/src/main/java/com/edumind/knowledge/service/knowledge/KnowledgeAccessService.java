package com.edumind.knowledge.service.knowledge;

import com.edumind.common.api.ResultCode;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.support.InternalInvocationContext;
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
            // 语义上是「资源不存在」而非服务故障：BusinessException(String) 的默认码是 500，
            // 会让一个正常的 404 场景在浏览器控制台表现成 Internal Server Error
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "知识库不存在");
        }
        assertCourseAccessible(knowledgeBase.getCourseId());
        return knowledgeBase;
    }

    public void assertCourseAccessible(Long courseId) {
        // 内部流水线（解析→切片→向量化）在异步线程中执行，没有登录用户上下文，
        // 这里的断言只面向用户发起的请求；内部调用方已在上游完成鉴权
        if (InternalInvocationContext.isInternal()) {
            return;
        }
        Long userId = resolveUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录");
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
        Long userId = resolveUserId();
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
        if (isAdmin()) {
            return knowledgeBaseDao.findAll();
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

    private Long resolveUserId() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            return userId;
        }
        try {
            if (cn.dev33.satoken.stp.StpUtil.isLogin()) {
                return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isAdmin() {
        LoginUser user = UserContext.get();
        if (user != null && user.getRoles() != null) {
            return user.getRoles().contains(SecurityConstant.ROLE_ADMIN);
        }
        try {
            if (cn.dev33.satoken.stp.StpUtil.isLogin()) {
                Long loginId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
                return Long.valueOf(1L).equals(loginId)
                        || cn.dev33.satoken.stp.StpUtil.hasRole(SecurityConstant.ROLE_ADMIN);
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
