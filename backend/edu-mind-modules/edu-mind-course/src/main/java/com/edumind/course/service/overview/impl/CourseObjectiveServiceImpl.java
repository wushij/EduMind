package com.edumind.course.service.overview.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseOverviewConverter;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseLearningObjectiveDao;
import com.edumind.course.dto.overview.CourseObjectiveItemDTO;
import com.edumind.course.dto.overview.CourseObjectivesSaveDTO;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.CourseLearningObjectiveEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.overview.CourseObjectiveService;
import com.edumind.course.vo.overview.CourseObjectiveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseObjectiveServiceImpl implements CourseObjectiveService {

    private static final int MAX_OBJECTIVES = 6;

    private final CourseDao courseDao;
    private final CourseLearningObjectiveDao objectiveDao;
    private final CourseOverviewConverter converter;
    private final CourseAccessService courseAccessService;

    @Override
    public List<CourseObjectiveVO> list(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanView(course);
        return objectiveDao.listByCourseId(courseId).stream()
                .map(converter::toObjectiveVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(Long courseId, CourseObjectivesSaveDTO dto) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
        List<CourseObjectiveItemDTO> items = dto.getObjectives() != null ? dto.getObjectives() : Collections.emptyList();
        if (items.size() > MAX_OBJECTIVES) {
            throw new BusinessException("教学目标最多" + MAX_OBJECTIVES + "条");
        }
        objectiveDao.deleteByCourseId(courseId);
        Long tenantId = course.getTenantId() != null ? course.getTenantId() : TenantContext.requireTenantId();
        int order = 1;
        for (CourseObjectiveItemDTO item : items) {
            if (item == null || !org.springframework.util.StringUtils.hasText(item.getTitle())) {
                continue;
            }
            CourseLearningObjectiveEntity entity = new CourseLearningObjectiveEntity();
            entity.setTenantId(tenantId);
            entity.setCourseId(courseId);
            entity.setSortOrder(order++);
            entity.setTitle(item.getTitle().trim());
            entity.setDescription(item.getDescription());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            objectiveDao.insert(entity);
        }
    }

    private CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }
}
