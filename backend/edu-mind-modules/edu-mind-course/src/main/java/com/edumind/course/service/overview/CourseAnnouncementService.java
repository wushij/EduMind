package com.edumind.course.service.overview;

import com.edumind.common.api.PageResult;
import com.edumind.course.dto.overview.CourseAnnouncementCreateDTO;
import com.edumind.course.dto.overview.CourseAnnouncementUpdateDTO;
import com.edumind.course.vo.overview.CourseAnnouncementVO;

import java.util.List;

public interface CourseAnnouncementService {
    List<CourseAnnouncementVO> listPreview(Long courseId, int limit);

    long countPublished(Long courseId);

    PageResult<CourseAnnouncementVO> page(Long courseId, long page, long pageSize, String status);

    CourseAnnouncementVO create(Long courseId, CourseAnnouncementCreateDTO dto);

    CourseAnnouncementVO update(Long courseId, Long announcementId, CourseAnnouncementUpdateDTO dto);

    void withdraw(Long courseId, Long announcementId);
}
