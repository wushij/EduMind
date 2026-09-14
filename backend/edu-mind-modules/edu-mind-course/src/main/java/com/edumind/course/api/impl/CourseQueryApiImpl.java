package com.edumind.course.api.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.dao.KnowledgePointDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseQueryApiImpl implements CourseQueryApi {

    private final CourseDao courseDao;
    private final ChapterDao chapterDao;
    private final CourseMemberDao courseMemberDao;
    private final KnowledgePointDao knowledgePointDao;
    private final CourseConverter courseConverter;
    private final UserQueryApi userQueryApi;

    @Override
    public CourseDetailVO getCourseById(Long courseId) {
        CourseEntity entity = courseDao.findById(courseId);
        if (entity == null) {
            return null;
        }
        return courseConverter.toDetailVO(
                entity,
                resolveTeacherName(entity.getTeacherId()),
                courseMemberDao.countStudentsByCourseId(entity.getId()));
    }

    @Override
    public List<CourseVO> listCoursesByIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return courseDao.findByIds(courseIds).stream()
                .map(entity -> courseConverter.toVO(
                        entity,
                        resolveTeacherName(entity.getTeacherId()),
                        courseMemberDao.countStudentsByCourseId(entity.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChapterTreeVO> listChaptersByCourseId(Long courseId) {
        return courseConverter.toChapterTree(chapterDao.findByCourseId(courseId));
    }

    @Override
    public long countCourses() {
        return courseDao.countAll();
    }

    @Override
    public List<KnowledgePointVO> listKnowledgePointsByCourseId(Long courseId) {
        return courseConverter.toKnowledgePointVOList(knowledgePointDao.findByCourseId(courseId, null));
    }

    @Override
    public KnowledgePointVO getKnowledgePointById(Long id) {
        return courseConverter.toKnowledgePointVO(knowledgePointDao.findById(id));
    }

    @Override
    public List<CourseBriefVO> listRecentCourses(int limit) {
        return courseDao.findRecent(limit).stream()
                .map(entity -> {
                    CourseBriefVO brief = new CourseBriefVO();
                    brief.setId(entity.getId());
                    brief.setTitle(entity.getTitle());
                    brief.setCode(entity.getCode());
                    brief.setLastVisitAt(entity.getUpdateTime());
                    return brief;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isCourseMember(Long courseId, Long userId) {
        if (courseId == null || userId == null) {
            return false;
        }
        CourseEntity course = courseDao.findById(courseId);
        if (course != null && userId.equals(course.getTeacherId())) {
            return true;
        }
        return courseMemberDao.findByCourseIdAndUserId(courseId, userId) != null;
    }

    @Override
    public List<Long> listCourseIdsByUserId(Long userId) {
        return courseMemberDao.findCourseIdsByUserId(userId);
    }

    @Override
    public List<Long> listStudentUserIdsByCourseId(Long courseId) {
        return courseMemberDao.findStudentUserIdsByCourseId(courseId);
    }

    private String resolveTeacherName(Long teacherId) {
        if (teacherId == null) {
            return "";
        }
        UserVO teacher = (UserVO) userQueryApi.getUserById(teacherId);
        if (teacher == null) {
            return "";
        }
        return teacher.getRealName() != null ? teacher.getRealName() : teacher.getUsername();
    }
}
