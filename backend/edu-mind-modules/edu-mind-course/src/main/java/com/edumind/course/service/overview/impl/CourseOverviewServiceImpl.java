package com.edumind.course.service.overview.impl;

import com.edumind.course.converter.CourseOverviewConverter;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.course.CourseService;
import com.edumind.course.service.overview.CourseAnnouncementService;
import com.edumind.course.service.overview.CourseInstructorProfileService;
import com.edumind.course.service.overview.CourseObjectiveService;
import com.edumind.course.service.overview.CourseOverviewService;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.overview.CourseOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseOverviewServiceImpl implements CourseOverviewService {

    private static final int ANNOUNCEMENT_PREVIEW_LIMIT = 5;

    private final CourseDao courseDao;
    private final CourseService courseService;
    private final CourseObjectiveService objectiveService;
    private final CourseAnnouncementService announcementService;
    private final CourseInstructorProfileService instructorProfileService;
    private final CourseOverviewConverter converter;
    private final CourseAccessService courseAccessService;

    @Override
    public CourseOverviewVO getOverview(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new com.edumind.common.exception.BusinessException("课程不存在");
        }
        courseAccessService.assertCanView(course);
        CourseDetailVO detail = courseService.getCourseById(courseId);
        return CourseOverviewVO.builder()
                .course(detail)
                .objectives(objectiveService.list(courseId))
                .announcementsPreview(announcementService.listPreview(courseId, ANNOUNCEMENT_PREVIEW_LIMIT))
                .announcementTotal(announcementService.countPublished(courseId))
                .instructors(instructorProfileService.listCards(courseId))
                .capabilityTags(converter.buildCapabilityTags(detail))
                .editable(courseAccessService.canEdit(course))
                .build();
    }
}
