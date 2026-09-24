package com.edumind.course.api.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseAccessApi;
import com.edumind.course.api.CourseDataScope;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.service.access.CourseAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseAccessApiImpl implements CourseAccessApi {

    private final CourseDao courseDao;
    private final CourseAccessService courseAccessService;

    @Override
    public void assertCanView(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanView(course);
    }

    @Override
    public void assertCanEdit(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
    }

    @Override
    public CourseDataScope resolveCurrentDataScope() {
        return courseAccessService.resolveCurrentDataScope();
    }

    @Override
    public boolean isCourseVisible(Long courseId) {
        return courseAccessService.isCourseVisible(courseId);
    }

    private CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }
}
