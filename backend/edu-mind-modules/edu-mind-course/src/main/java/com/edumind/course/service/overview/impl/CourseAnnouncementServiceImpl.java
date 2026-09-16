package com.edumind.course.service.overview.impl;

import com.edumind.common.api.PageResult;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseOverviewConverter;
import com.edumind.course.dao.CourseAnnouncementDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.dto.overview.CourseAnnouncementCreateDTO;
import com.edumind.course.dto.overview.CourseAnnouncementUpdateDTO;
import com.edumind.course.entity.CourseAnnouncementEntity;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.overview.CourseAnnouncementService;
import com.edumind.course.vo.overview.CourseAnnouncementVO;
import com.edumind.notification.api.NotificationWriteApi;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseAnnouncementServiceImpl implements CourseAnnouncementService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String NOTIFY_TYPE_COURSE = "COURSE";

    private final CourseDao courseDao;
    private final CourseAnnouncementDao announcementDao;
    private final CourseMemberDao courseMemberDao;
    private final CourseOverviewConverter converter;
    private final CourseAccessService courseAccessService;
    private final NotificationWriteApi notificationWriteApi;
    private final UserQueryApi userQueryApi;

    @Override
    public List<CourseAnnouncementVO> listPreview(Long courseId, int limit) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanView(course);
        return announcementDao.listPreview(courseId, limit).stream()
                .map(converter::toAnnouncementVO)
                .collect(Collectors.toList());
    }

    @Override
    public long countPublished(Long courseId) {
        return announcementDao.countPublishedByCourseId(courseId);
    }

    @Override
    public PageResult<CourseAnnouncementVO> page(Long courseId, long page, long pageSize, String status) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanView(course);
        PageResult<CourseAnnouncementEntity> raw = announcementDao.pageByCourseId(courseId, page, pageSize, status);
        List<CourseAnnouncementVO> list = raw.getList().stream()
                .map(converter::toAnnouncementVO)
                .collect(Collectors.toList());
        return PageResult.<CourseAnnouncementVO>builder()
                .total(raw.getTotal())
                .pageNum(raw.getPageNum())
                .pageSize(raw.getPageSize())
                .list(list)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseAnnouncementVO create(Long courseId, CourseAnnouncementCreateDTO dto) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
        Long publisherId = LoginUserResolver.requireUserId();
        UserBriefVO publisher = userQueryApi.getUserById(publisherId);
        String publisherName = publisher != null
                ? (StringUtils.hasText(publisher.getRealName()) ? publisher.getRealName() : publisher.getUsername())
                : "";

        CourseAnnouncementEntity entity = new CourseAnnouncementEntity();
        entity.setTenantId(course.getTenantId() != null ? course.getTenantId() : TenantContext.requireTenantId());
        entity.setCourseId(courseId);
        entity.setTitle(dto.getTitle().trim());
        entity.setContent(dto.getContent().trim());
        entity.setPinned(Boolean.TRUE.equals(dto.getPinned()));
        entity.setStatus(STATUS_PUBLISHED);
        entity.setPublishTime(LocalDateTime.now());
        entity.setPublisherId(publisherId);
        entity.setPublisherName(publisherName);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        announcementDao.insert(entity);

        notifyCourseMembers(course, entity);
        return converter.toAnnouncementVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseAnnouncementVO update(Long courseId, Long announcementId, CourseAnnouncementUpdateDTO dto) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
        CourseAnnouncementEntity entity = announcementDao.findByIdAndCourseId(announcementId, courseId);
        if (entity == null) {
            throw new BusinessException("公告不存在");
        }
        if (StringUtils.hasText(dto.getTitle())) {
            entity.setTitle(dto.getTitle().trim());
        }
        if (dto.getContent() != null) {
            entity.setContent(dto.getContent().trim());
        }
        if (dto.getPinned() != null) {
            entity.setPinned(dto.getPinned());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus().trim().toUpperCase());
        }
        entity.setUpdateTime(LocalDateTime.now());
        announcementDao.updateById(entity);
        return converter.toAnnouncementVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long courseId, Long announcementId) {
        CourseAnnouncementUpdateDTO dto = new CourseAnnouncementUpdateDTO();
        dto.setStatus(STATUS_WITHDRAWN);
        update(courseId, announcementId, dto);
    }

    private void notifyCourseMembers(CourseEntity course, CourseAnnouncementEntity entity) {
        Set<Long> userIds = new HashSet<>(courseMemberDao.findAllUserIdsByCourseId(course.getId()));
        if (course.getTeacherId() != null) {
            userIds.add(course.getTeacherId());
        }
        if (userIds.isEmpty()) {
            return;
        }
        Long tenantId = course.getTenantId() != null ? course.getTenantId() : TenantContext.requireTenantId();
        notificationWriteApi.sendToUsers(
                tenantId,
                new ArrayList<>(userIds),
                entity.getTitle(),
                entity.getContent(),
                NOTIFY_TYPE_COURSE,
                course.getId());
    }

    private CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }
}
