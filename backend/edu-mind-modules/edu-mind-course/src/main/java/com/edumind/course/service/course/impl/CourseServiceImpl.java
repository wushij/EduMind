package com.edumind.course.service.course.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.enums.RoleCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.dao.KnowledgePointDao;
import com.edumind.course.dto.course.CourseCreateDTO;
import com.edumind.course.dto.course.CourseQueryDTO;
import com.edumind.course.dto.course.CourseUpdateDTO;
import com.edumind.course.dto.knowledge.KnowledgePointCreateDTO;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.KnowledgePointEntity;
import com.edumind.course.service.course.CourseService;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseDao courseDao;
    private final CourseMemberDao courseMemberDao;
    private final KnowledgePointDao knowledgePointDao;
    private final CourseConverter courseConverter;
    private final UserQueryApi userQueryApi;

    @Override
    public PageResult<CourseVO> listCourses(CourseQueryDTO query) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<String> roles = userQueryApi.getRolesByUserId(currentUserId);

        Page<CourseEntity> page;
        if (roles.contains(RoleCode.ADMIN.getCode())) {
            page = courseDao.pageQueryAll(query);
        } else if (roles.contains(RoleCode.TEACHER.getCode())) {
            List<Long> teacherCourseIds = courseDao.findByTeacherId(currentUserId).stream()
                    .map(CourseEntity::getId)
                    .collect(Collectors.toList());
            if (teacherCourseIds.isEmpty()) {
                return PageResult.empty(defaultPage(query), defaultPageSize(query));
            }
            page = courseDao.pageQuery(query, teacherCourseIds);
        } else if (roles.contains(RoleCode.STUDENT.getCode())) {
            List<Long> enrolledCourseIds = courseMemberDao.findCourseIdsByUserId(currentUserId);
            if (enrolledCourseIds.isEmpty()) {
                return PageResult.empty(defaultPage(query), defaultPageSize(query));
            }
            page = courseDao.pageQuery(query, enrolledCourseIds);
        } else {
            return PageResult.empty(defaultPage(query), defaultPageSize(query));
        }

        List<CourseVO> list = page.getRecords().stream()
                .map(entity -> courseConverter.toVO(
                        entity,
                        resolveTeacherName(entity.getTeacherId()),
                        courseMemberDao.countStudentsByCourseId(entity.getId())))
                .collect(Collectors.toList());

        return PageResult.<CourseVO>builder()
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .list(list)
                .build();
    }

    @Override
    public CourseDetailVO getCourseById(Long id) {
        CourseEntity entity = courseDao.findById(id);
        if (entity == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseAccessible(entity);
        return courseConverter.toDetailVO(
                entity,
                resolveTeacherName(entity.getTeacherId()),
                courseMemberDao.countStudentsByCourseId(entity.getId()));
    }

    @Override
    public Long createCourse(CourseCreateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<String> roles = userQueryApi.getRolesByUserId(currentUserId);
        if (!roles.contains(RoleCode.ADMIN.getCode()) && !roles.contains(RoleCode.TEACHER.getCode())) {
            throw new BusinessException("无权限创建课程");
        }
        CourseEntity entity = courseConverter.toEntity(dto, currentUserId);
        courseDao.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateCourse(Long id, CourseUpdateDTO dto) {
        CourseEntity entity = courseDao.findById(id);
        if (entity == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseEditable(entity);
        courseConverter.applyUpdate(entity, dto);
        courseDao.updateById(entity);
    }

    @Override
    public void deleteCourse(Long id) {
        CourseEntity entity = courseDao.findById(id);
        if (entity == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseEditable(entity);
        entity.setStatus(0);
        courseDao.updateById(entity);
    }

    @Override
    public List<KnowledgePointVO> listKnowledgePoints(Long courseId, Long chapterId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseAccessible(course);
        return courseConverter.toKnowledgePointVOList(
                knowledgePointDao.findByCourseId(courseId, chapterId));
    }

    @Override
    public KnowledgePointVO createKnowledgePoint(Long courseId, KnowledgePointCreateDTO dto) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseEditable(course);

        KnowledgePointEntity entity = new KnowledgePointEntity();
        entity.setCourseId(courseId);
        entity.setChapterId(dto.getChapterId());
        entity.setTitle(dto.getTitle().trim());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setCreateTime(LocalDateTime.now());

        knowledgePointDao.insert(entity);
        return courseConverter.toKnowledgePointVO(entity);
    }

    private void assertCourseEditable(CourseEntity course) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<String> roles = userQueryApi.getRolesByUserId(currentUserId);
        if (roles.contains(RoleCode.ADMIN.getCode())) {
            return;
        }
        if (roles.contains(RoleCode.TEACHER.getCode()) && currentUserId.equals(course.getTeacherId())) {
            return;
        }
        throw new BusinessException("无权限编辑该课程");
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

    private long defaultPage(CourseQueryDTO query) {
        return query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1L;
    }

    private long defaultPageSize(CourseQueryDTO query) {
        return query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10L;
    }
}
