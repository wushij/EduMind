package com.edumind.course.service.access;

import com.edumind.common.context.TenantContext;
import com.edumind.common.enums.RoleCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseDataScope;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.CourseMemberEntity;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.api.TenantDataScope;
import com.edumind.system.api.TenantDataScopeApi;
import com.edumind.system.api.UserQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseAccessService {

    private final CourseDao courseDao;
    private final CourseMemberDao courseMemberDao;
    private final UserQueryApi userQueryApi;
    private final TenantDataScopeApi tenantDataScopeApi;
    private final OrganizationQueryApi organizationQueryApi;

    public CourseEntity assertCanViewByCourseId(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        assertCanView(course);
        return course;
    }

    public CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }

    public void assertCanView(CourseEntity course) {
        Long currentUserId = LoginUserResolver.requireUserId();
        List<String> roles = userQueryApi.getRolesByUserId(currentUserId);
        if (roles.contains(RoleCode.ADMIN.getCode())
                || roles.contains("PLATFORM_ADMIN")
                || roles.contains(RoleCode.TENANT_ADMIN.getCode())) {
            return;
        }
        if (currentUserId.equals(course.getTeacherId())) {
            return;
        }
        CourseMemberEntity member = courseMemberDao.findByCourseIdAndUserId(course.getId(), currentUserId);
        if (member != null) {
            return;
        }
        throw new BusinessException("无权限访问该课程");
    }

    public boolean canEdit(CourseEntity course) {
        try {
            assertCanEdit(course);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    /**
     * 课程门户编辑权限：仅课程创建者（{@code teacher_id}）、本课教师/助教成员可维护。
     * 平台管理员查看他人课程时不可编辑；创建课程时创建者会写入 {@code teacher_id}。
     * 不因全局 {@code course:edit} 或「仅选课学生」身份获得编辑权。
     */
    public void assertCanEdit(CourseEntity course) {
        Long currentUserId = LoginUserResolver.requireUserId();
        if (course.getTeacherId() != null && course.getTeacherId().equals(currentUserId)) {
            return;
        }
        CourseMemberEntity member = courseMemberDao.findByCourseIdAndUserId(course.getId(), currentUserId);
        if (member != null && ("TEACHER".equals(member.getMemberRole()) || "ASSISTANT".equals(member.getMemberRole()))) {
            return;
        }
        throw new BusinessException("无权限编辑该课程，仅课程创建者或本课教师/助教可维护");
    }

    /**
     * 解析当前登录用户的课程可见范围。
     *
     * <p>这是课程模块对外唯一的「可见课程集合」口径：课程中心列表、作业列表、答卷列表
     * 等一切按课程收敛的查询都必须复用它，避免各模块各写一套过滤条件而导致越权可见。</p>
     */
    public CourseDataScope resolveCurrentDataScope() {
        Long currentUserId = LoginUserResolver.requireUserId();
        Long tenantId = TenantContext.getTenantId();
        TenantDataScope scope = tenantDataScopeApi.resolve(currentUserId, tenantId);

        // 平台超管 / 租户管理员：租户内全量可见
        if (scope.isAllTenant()) {
            return CourseDataScope.all();
        }

        List<String> roles = userQueryApi.getRolesByUserId(currentUserId);

        // 纯学生：课程中心仅展示已加入（选课）的课程，不按院系扩散教师开课
        if (isStudentOnlyRole(roles)) {
            return CourseDataScope.of(courseMemberDao.findCourseIdsByUserId(currentUserId));
        }

        Set<Long> allowedCourseIds = new HashSet<>(scope.getCourseIds());
        if (roles != null && roles.contains(RoleCode.TEACHER.getCode())) {
            allowedCourseIds.addAll(listCourseIdsByTeacherId(currentUserId));
        }
        if (roles != null && roles.contains(RoleCode.STUDENT.getCode())) {
            allowedCourseIds.addAll(courseMemberDao.findCourseIdsByUserId(currentUserId));
        }
        // 院系管理员 / 教研负责人：包含所属组织及子树下成员所授课程（学生身份不进入此分支）
        if (canExpandOrgTeacherCourses(roles)
                && scope.getOrgIds() != null
                && !scope.getOrgIds().isEmpty()) {
            for (Long orgId : scope.getOrgIds()) {
                List<Long> userIds = organizationQueryApi.listUserIdsByOrgId(tenantId, orgId);
                if (CollectionUtils.isEmpty(userIds)) {
                    continue;
                }
                for (Long uid : userIds) {
                    allowedCourseIds.addAll(listCourseIdsByTeacherId(uid));
                }
            }
        }
        return CourseDataScope.of(allowedCourseIds);
    }

    /** 指定课程是否在当前用户可见范围内（口径与课程中心列表一致） */
    public boolean isCourseVisible(Long courseId) {
        return resolveCurrentDataScope().contains(courseId);
    }

    private List<Long> listCourseIdsByTeacherId(Long teacherId) {
        return courseDao.findByTeacherId(teacherId).stream()
                .map(CourseEntity::getId)
                .toList();
    }

    /** 仅学生身份（无管理/教师全局角色） */
    private boolean isStudentOnlyRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        if (roles.contains(RoleCode.ADMIN.getCode())
                || roles.contains("PLATFORM_ADMIN")
                || roles.contains(RoleCode.TENANT_ADMIN.getCode())
                || roles.contains(RoleCode.ORG_ADMIN.getCode())
                || roles.contains(RoleCode.TEACHER.getCode())) {
            return false;
        }
        return roles.contains(RoleCode.STUDENT.getCode());
    }

    private boolean canExpandOrgTeacherCourses(List<String> roles) {
        if (roles == null) {
            return false;
        }
        return roles.contains(RoleCode.ORG_ADMIN.getCode());
    }
}
