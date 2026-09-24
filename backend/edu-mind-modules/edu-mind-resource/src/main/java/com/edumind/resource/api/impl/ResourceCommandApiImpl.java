package com.edumind.resource.api.impl;

import com.edumind.resource.api.ResourceCommandApi;
import com.edumind.resource.dao.CourseResourceDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceCommandApiImpl implements ResourceCommandApi {

    private final CourseResourceDao courseResourceDao;

    @Override
    public void deleteResourcesByCourseId(Long courseId) {
        if (courseId == null) {
            return;
        }
        int count = courseResourceDao.deleteByCourseId(courseId);
        log.info("级联清理课程关联资源成功: courseId={}, deletedCount={}", courseId, count);
    }
}
