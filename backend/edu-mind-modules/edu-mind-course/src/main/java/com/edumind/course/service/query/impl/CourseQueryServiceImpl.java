package com.edumind.course.service.query.impl;

import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.dao.KnowledgePointDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.KnowledgePointEntity;
import com.edumind.course.service.knowledge.KnowledgePointService;
import com.edumind.course.service.query.CourseQueryService;
import com.edumind.course.vo.CourseBriefVO;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseQueryServiceImpl implements CourseQueryService {

    private final CourseDao courseDao;
    private final ChapterDao chapterDao;
    private final CourseMemberDao courseMemberDao;
    private final KnowledgePointDao knowledgePointDao;
    private final CourseConverter courseConverter;
    private final UserQueryApi userQueryApi;
    private final KnowledgePointService knowledgePointService;

    @Override
    public CourseDetailVO getCourseById(Long courseId) {
        CourseEntity entity = courseDao.findById(courseId);
        if (entity == null) {
            return null;
        }
        UserBriefVO teacher = resolveTeacher(entity.getTeacherId());
        return courseConverter.toDetailVO(
                entity,
                resolveTeacherName(teacher),
                resolveTeacherAvatar(teacher),
                courseMemberDao.countStudentsByCourseId(entity.getId()));
    }

    @Override
    public List<CourseVO> listCoursesByIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return courseDao.findByIds(courseIds).stream()
                .map(entity -> {
                    UserBriefVO teacher = resolveTeacher(entity.getTeacherId());
                    return courseConverter.toVO(
                            entity,
                            resolveTeacherName(teacher),
                            resolveTeacherAvatar(teacher),
                            courseMemberDao.countStudentsByCourseId(entity.getId()));
                })
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
        return knowledgePointService.listByCourse(courseId, null);
    }

    @Override
    public KnowledgePointVO getKnowledgePointById(Long id) {
        KnowledgePointEntity entity = knowledgePointDao.findById(id);
        if (entity == null) {
            return null;
        }
        return courseConverter.toKnowledgePointVO(entity);
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

    private UserBriefVO resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        return userQueryApi.getUserById(teacherId);
    }

    private String resolveTeacherName(UserBriefVO teacher) {
        if (teacher == null) {
            return "";
        }
        return teacher.getRealName() != null ? teacher.getRealName() : teacher.getUsername();
    }

    private String resolveTeacherAvatar(UserBriefVO teacher) {
        if (teacher == null || teacher.getAvatar() == null || teacher.getAvatar().isBlank()) {
            return null;
        }
        return teacher.getAvatar();
    }
}
