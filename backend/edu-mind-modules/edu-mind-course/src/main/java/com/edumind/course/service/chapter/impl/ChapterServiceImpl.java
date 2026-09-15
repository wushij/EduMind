package com.edumind.course.service.chapter.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.service.chapter.ChapterService;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.common.enums.RoleCode;
import com.edumind.system.api.UserQueryApi;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final CourseDao courseDao;
    private final ChapterDao chapterDao;
    private final CourseMemberDao courseMemberDao;
    private final CourseConverter courseConverter;
    private final UserQueryApi userQueryApi;

    @Override
    public List<ChapterTreeVO> getChapterTree(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseAccessible(course);
        return courseConverter.toChapterTree(chapterDao.findByCourseId(courseId));
    }

    @Override
    public Long createChapter(Long courseId, String title, Long parentId, Integer sortOrder) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<String> roles = userQueryApi.getRolesByUserId(currentUserId);
        if (!roles.contains(RoleCode.ADMIN.getCode()) && !currentUserId.equals(course.getTeacherId())) {
            throw new BusinessException("无权限为该课程添加章节");
        }
        com.edumind.course.entity.ChapterEntity chapter = new com.edumind.course.entity.ChapterEntity();
        chapter.setCourseId(courseId);
        chapter.setTitle(title);
        chapter.setParentId(parentId != null ? parentId : 0L);
        chapter.setSortOrder(sortOrder != null ? sortOrder : 1);
        chapter.setCreateTime(java.time.LocalDateTime.now());
        chapterDao.insert(chapter);
        return chapter.getId();
    }

    private void assertCourseAccessible(CourseEntity course) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<String> roles = userQueryApi.getRolesByUserId(currentUserId);
        if (roles.contains(RoleCode.ADMIN.getCode())) {
            return;
        }
        if (roles.contains(RoleCode.TEACHER.getCode()) && currentUserId.equals(course.getTeacherId())) {
            return;
        }
        if (roles.contains(RoleCode.STUDENT.getCode())) {
            List<Long> enrolledCourseIds = courseMemberDao.findCourseIdsByUserId(currentUserId);
            if (enrolledCourseIds.contains(course.getId())) {
                return;
            }
        }
        throw new BusinessException("无权限访问该课程");
    }
}
