package com.edumind.course.service.course.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseDataScope;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.dao.KnowledgePointDao;
import com.edumind.course.dto.course.CourseCreateDTO;
import com.edumind.course.dto.course.CourseQueryDTO;
import com.edumind.course.dto.course.CourseUpdateDTO;
import com.edumind.course.dto.knowledge.KnowledgePointCreateDTO;
import com.edumind.course.dto.knowledge.KnowledgePointUpdateDTO;
import com.edumind.course.service.knowledge.KnowledgePointService;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.CourseMemberEntity;
import com.edumind.course.entity.KnowledgePointEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.course.CourseService;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import com.edumind.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseDao courseDao;
    private final CourseMemberDao courseMemberDao;
    private final KnowledgePointDao knowledgePointDao;
    private final com.edumind.course.dao.ChapterDao chapterDao;
    private final com.edumind.resource.api.ResourceQueryApi resourceQueryApi;
    private final com.edumind.resource.api.ResourceCommandApi resourceCommandApi;
    private final CourseConverter courseConverter;
    private final UserQueryApi userQueryApi;
    private final CourseAccessService courseAccessService;
    private final KnowledgePointService knowledgePointService;

    @Override
    public PageResult<CourseVO> listCourses(CourseQueryDTO query) {
        // 可见课程范围统一由 CourseAccessService 解析：作业/答卷/学情等模块共用同一口径，
        // 禁止在此处另写一套过滤条件（曾经课程中心过滤、作业列表全量返回，导致越权可见）。
        CourseDataScope dataScope = courseAccessService.resolveCurrentDataScope();

        Page<CourseEntity> page;
        if (dataScope.isAll()) {
            page = courseDao.pageQueryAll(query);
        } else {
            if (dataScope.isEmpty()) {
                return PageResult.empty(defaultPage(query), defaultPageSize(query));
            }
            page = courseDao.pageQuery(query, new ArrayList<>(dataScope.getCourseIds()));
        }

        Map<Long, UserBriefVO> teacherCache = new HashMap<>();
        List<CourseVO> list = page.getRecords().stream()
                .map(entity -> {
                    Long cid = entity.getId();
                    Long studentCount = courseMemberDao.countStudentsByCourseId(cid);
                    Long chapterCount = chapterDao.countByCourseId(cid);
                    Long kpCount = knowledgePointDao.countByCourseId(cid);
                    Long resourceCount = resourceQueryApi != null ? resourceQueryApi.countResourcesByCourseId(cid) : 0L;
                    UserBriefVO teacher = entity.getTeacherId() != null
                            ? teacherCache.computeIfAbsent(entity.getTeacherId(), this::resolveTeacher)
                            : null;
                    return courseConverter.toVO(
                            entity,
                            resolveTeacherName(teacher),
                            resolveTeacherAvatar(teacher),
                            studentCount,
                            chapterCount,
                            kpCount,
                            resourceCount);
                })
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
        Long studentCount = courseMemberDao.countStudentsByCourseId(entity.getId());
        Long chapterCount = chapterDao.countByCourseId(entity.getId());
        Long kpCount = knowledgePointDao.countByCourseId(entity.getId());
        Long resourceCount = resourceQueryApi != null ? resourceQueryApi.countResourcesByCourseId(entity.getId()) : 0L;
        UserBriefVO teacher = resolveTeacher(entity.getTeacherId());
        CourseDetailVO detail = courseConverter.toDetailVO(
                entity,
                resolveTeacherName(teacher),
                resolveTeacherAvatar(teacher),
                studentCount,
                chapterCount,
                kpCount,
                resourceCount);
        detail.setEditable(courseAccessService.canEdit(entity));
        return detail;
    }

    @Override
    public Long createCourse(CourseCreateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!StpUtil.hasPermission("course:create")) {
            throw new BusinessException("无权限创建课程");
        }
        CourseEntity entity = courseConverter.toEntity(dto, currentUserId);
        if (entity.getTenantId() == null) {
            entity.setTenantId(TenantContext.requireTenantId());
        }
        courseDao.insert(entity);
        Long newCourseId = entity.getId();

        // 1. 自动将创建教师录入为课程班级主讲教师（全链路成员互通）
        try {
            CourseMemberEntity teacherMember = new CourseMemberEntity();
            teacherMember.setCourseId(newCourseId);
            teacherMember.setUserId(currentUserId);
            teacherMember.setMemberRole("TEACHER");
            teacherMember.setCreateTime(LocalDateTime.now());
            courseMemberDao.insert(teacherMember);
        } catch (Exception ignored) {
            // 忽略非致命写入异常
        }

        // 2. 初始化大纲章节（全链路章节互通，杜绝空壳 0 章节）
        if (dto.getInitialChapters() != null && !dto.getInitialChapters().isEmpty()) {
            int sort = 1;
            for (String chapTitle : dto.getInitialChapters()) {
                if (chapTitle != null && !chapTitle.trim().isEmpty()) {
                    ChapterEntity chapter = new ChapterEntity();
                    chapter.setCourseId(newCourseId);
                    chapter.setParentId(0L);
                    chapter.setTitle(chapTitle.trim());
                    chapter.setSortOrder(sort++);
                    chapter.setCreateTime(LocalDateTime.now());
                    chapterDao.insert(chapter);
                }
            }
        }

        return newCourseId;
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
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long id) {
        CourseEntity entity = courseDao.findById(id);
        if (entity == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseEditable(entity);

        // 安全防线：检查是否已有学生选课修读（以学生身份加入）
        long studentCount = courseMemberDao.countStudentsByCourseId(id);
        if (studentCount > 0) {
            throw new BusinessException("该课程已有学生选课修读，为保护学业档案严禁删除，仅支持结课归档");
        }

        // 1. 级联清理章节
        chapterDao.deleteByCourseId(id);

        // 2. 级联清理知识点
        knowledgePointDao.deleteByCourseId(id);

        // 3. 级联清理成员记录（如创建教师/助教自身关联）
        courseMemberDao.deleteByCourseId(id);

        // 4. 级联清理课程关联资源
        if (resourceCommandApi != null) {
            resourceCommandApi.deleteResourcesByCourseId(id);
        }

        // 5. 彻底删除课程实体
        courseDao.deleteById(id);
    }

    @Override
    public void archiveCourse(Long id) {
        CourseEntity entity = courseDao.findById(id);
        if (entity == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseEditable(entity);
        entity.setStatus(0);
        courseDao.updateById(entity);
    }

    @Override
    public void unarchiveCourse(Long id) {
        CourseEntity entity = courseDao.findById(id);
        if (entity == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseEditable(entity);
        entity.setStatus(1);
        courseDao.updateById(entity);
    }

    @Override
    public List<KnowledgePointVO> listKnowledgePoints(Long courseId, Long chapterId) {
        return knowledgePointService.listByCourse(courseId, chapterId);
    }

    @Override
    public KnowledgePointVO getKnowledgePoint(Long courseId, Long kpId) {
        return knowledgePointService.getById(courseId, kpId);
    }

    @Override
    public KnowledgePointVO createKnowledgePoint(Long courseId, KnowledgePointCreateDTO dto) {
        return knowledgePointService.create(courseId, dto);
    }

    @Override
    public KnowledgePointVO updateKnowledgePoint(Long courseId, Long kpId, KnowledgePointUpdateDTO dto) {
        return knowledgePointService.update(courseId, kpId, dto);
    }

    @Override
    public void deleteKnowledgePoint(Long courseId, Long kpId) {
        knowledgePointService.delete(courseId, kpId);
    }

    @Override
    public Long joinCourseByCode(String code) {
        if (!org.springframework.util.StringUtils.hasText(code)) {
            throw new BusinessException("请输入有效的课程代码");
        }
        CourseEntity course = courseDao.findByCode(code.trim());
        if (course == null) {
            try {
                Long courseId = Long.parseLong(code.trim());
                course = courseDao.findById(courseId);
            } catch (NumberFormatException ignored) {}
        }
        if (course == null) {
            throw new BusinessException("未找到匹配的课程，请核对课程代号");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (courseMemberDao.findByCourseIdAndUserId(course.getId(), currentUserId) != null) {
            throw new BusinessException("您已在此课程班级中，无需重复加入");
        }
        com.edumind.course.entity.CourseMemberEntity member = new com.edumind.course.entity.CourseMemberEntity();
        member.setCourseId(course.getId());
        member.setUserId(currentUserId);
        member.setMemberRole("STUDENT");
        courseMemberDao.insert(member);
        return course.getId();
    }

    @Override
    public List<CourseVO> listPublicCourses() {
        CourseQueryDTO query = new CourseQueryDTO();
        query.setPage(1L);
        query.setPageSize(20L);
        Page<CourseEntity> page = courseDao.pageQueryAll(query);
        return page.getRecords().stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .map(entity -> {
                    Long cid = entity.getId();
                    Long studentCount = courseMemberDao.countStudentsByCourseId(cid);
                    Long chapterCount = chapterDao.countByCourseId(cid);
                    Long kpCount = knowledgePointDao.countByCourseId(cid);
                    Long resourceCount = resourceQueryApi != null ? resourceQueryApi.countResourcesByCourseId(cid) : 0L;
                    UserBriefVO teacher = resolveTeacher(entity.getTeacherId());
                    return courseConverter.toVO(
                            entity,
                            resolveTeacherName(teacher),
                            resolveTeacherAvatar(teacher),
                            studentCount,
                            chapterCount,
                            kpCount,
                            resourceCount);
                })
                .collect(Collectors.toList());
    }

    private void assertCourseEditable(CourseEntity course) {
        courseAccessService.assertCanEdit(course);
    }

    private void assertCourseAccessible(CourseEntity course) {
        courseAccessService.assertCanView(course);
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

    private long defaultPage(CourseQueryDTO query) {
        return query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1L;
    }

    private long defaultPageSize(CourseQueryDTO query) {
        return query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10L;
    }
}
