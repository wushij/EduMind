package com.edumind.course.service.overview.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseInstructorProfileDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.dto.overview.CourseInstructorProfileItemDTO;
import com.edumind.course.dto.overview.CourseInstructorsSaveDTO;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.CourseInstructorProfileEntity;
import com.edumind.course.entity.CourseMemberEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.overview.CourseInstructorProfileService;
import com.edumind.course.vo.overview.CourseInstructorCardVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseInstructorProfileServiceImpl implements CourseInstructorProfileService {

    private final CourseDao courseDao;
    private final CourseMemberDao courseMemberDao;
    private final CourseInstructorProfileDao profileDao;
    private final CourseAccessService courseAccessService;
    private final UserQueryApi userQueryApi;

    @Override
    public List<CourseInstructorCardVO> listCards(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanView(course);
        ensurePrimaryProfile(course);
        List<CourseInstructorProfileEntity> profiles = profileDao.listByCourseId(courseId);
        Map<Long, CourseMemberEntity> memberMap = courseMemberDao.findByCourseId(courseId).stream()
                .collect(Collectors.toMap(CourseMemberEntity::getUserId, m -> m, (a, b) -> a));
        List<CourseInstructorCardVO> cards = new ArrayList<>();
        for (CourseInstructorProfileEntity profile : profiles) {
            cards.add(toCard(profile, memberMap.get(profile.getUserId()), course));
        }
        cards.sort(Comparator
                .comparing((CourseInstructorCardVO c) -> !Boolean.TRUE.equals(c.getPrimary()))
                .thenComparing(c -> c.getSortOrder() != null ? c.getSortOrder() : 0));
        return cards;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(Long courseId, CourseInstructorsSaveDTO dto) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
        if (dto.getInstructors() == null) {
            return;
        }
        Long tenantId = course.getTenantId() != null ? course.getTenantId() : TenantContext.requireTenantId();
        for (CourseInstructorProfileItemDTO item : dto.getInstructors()) {
            if (item == null || item.getUserId() == null) {
                continue;
            }
            CourseInstructorProfileEntity existing = profileDao.findByCourseIdAndUserId(courseId, item.getUserId());
            if (existing == null) {
                existing = new CourseInstructorProfileEntity();
                existing.setTenantId(tenantId);
                existing.setCourseId(courseId);
                existing.setUserId(item.getUserId());
                existing.setCreateTime(LocalDateTime.now());
            }
            existing.setIntro(item.getIntro());
            existing.setOfficeHours(item.getOfficeHours());
            existing.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
            existing.setIsPrimary(Boolean.TRUE.equals(item.getPrimary())
                    || (course.getTeacherId() != null && course.getTeacherId().equals(item.getUserId())));
            existing.setUpdateTime(LocalDateTime.now());
            if (existing.getId() == null) {
                profileDao.insert(existing);
            } else {
                profileDao.updateById(existing);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromMembers(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
        ensurePrimaryProfile(course);
        Long tenantId = course.getTenantId() != null ? course.getTenantId() : TenantContext.requireTenantId();
        int order = 1;
        for (CourseMemberEntity member : courseMemberDao.findByCourseId(courseId)) {
            if (member.getUserId() == null) {
                continue;
            }
            String role = member.getMemberRole();
            if (!"TEACHER".equals(role) && !"ASSISTANT".equals(role)) {
                continue;
            }
            if (profileDao.findByCourseIdAndUserId(courseId, member.getUserId()) != null) {
                continue;
            }
            CourseInstructorProfileEntity profile = new CourseInstructorProfileEntity();
            profile.setTenantId(tenantId);
            profile.setCourseId(courseId);
            profile.setUserId(member.getUserId());
            profile.setSortOrder(order++);
            profile.setIsPrimary(course.getTeacherId() != null && course.getTeacherId().equals(member.getUserId()));
            profile.setCreateTime(LocalDateTime.now());
            profile.setUpdateTime(LocalDateTime.now());
            profileDao.insert(profile);
        }
    }

    private void ensurePrimaryProfile(CourseEntity course) {
        if (course.getTeacherId() == null) {
            return;
        }
        if (profileDao.findByCourseIdAndUserId(course.getId(), course.getTeacherId()) != null) {
            return;
        }
        CourseInstructorProfileEntity profile = new CourseInstructorProfileEntity();
        profile.setTenantId(course.getTenantId() != null ? course.getTenantId() : TenantContext.requireTenantId());
        profile.setCourseId(course.getId());
        profile.setUserId(course.getTeacherId());
        profile.setIsPrimary(true);
        profile.setSortOrder(0);
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        profileDao.insert(profile);
    }

    private CourseInstructorCardVO toCard(CourseInstructorProfileEntity profile, CourseMemberEntity member, CourseEntity course) {
        UserBriefVO user = userQueryApi.getUserById(profile.getUserId());
        String role = member != null ? member.getMemberRole() : (Boolean.TRUE.equals(profile.getIsPrimary()) ? "TEACHER" : "TEACHER");
        return CourseInstructorCardVO.builder()
                .userId(profile.getUserId())
                .username(user != null ? user.getUsername() : null)
                .realName(user != null ? user.getRealName() : null)
                .avatar(user != null ? user.getAvatar() : null)
                .memberRole(role)
                .roleLabel(resolveRoleLabel(role, course.getTeacherId(), profile.getUserId()))
                .intro(profile.getIntro())
                .officeHours(profile.getOfficeHours())
                .primary(Boolean.TRUE.equals(profile.getIsPrimary())
                        || (course.getTeacherId() != null && course.getTeacherId().equals(profile.getUserId())))
                .sortOrder(profile.getSortOrder())
                .build();
    }

    private String resolveRoleLabel(String role, Long teacherId, Long userId) {
        if (teacherId != null && teacherId.equals(userId)) {
            return "主讲教师";
        }
        if ("ASSISTANT".equals(role)) {
            return "助教";
        }
        if ("TEACHER".equals(role)) {
            return "授课教师";
        }
        return "教学团队";
    }

    private CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }
}
