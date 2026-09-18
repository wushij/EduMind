package com.edumind.course.service.access;

import com.edumind.common.enums.RoleCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.CourseMemberEntity;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.system.api.UserQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseAccessService {

    private final CourseDao courseDao;
    private final CourseMemberDao courseMemberDao;
    private final UserQueryApi userQueryApi;

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
        if (roles.contains(RoleCode.ADMIN.getCode())) {
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
}
