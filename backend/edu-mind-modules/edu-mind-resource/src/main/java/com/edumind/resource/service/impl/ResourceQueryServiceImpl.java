package com.edumind.resource.service.impl;

import com.edumind.resource.converter.ResourceConverter;
import com.edumind.resource.dao.CourseResourceDao;
import com.edumind.resource.dao.ResourceDao;
import com.edumind.resource.entity.ResourceEntity;
import com.edumind.resource.service.ResourceQueryService;
import com.edumind.resource.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceQueryServiceImpl implements ResourceQueryService {

    private final ResourceDao resourceDao;
    private final CourseResourceDao courseResourceDao;
    private final ResourceConverter resourceConverter;

    @Override
    public Long countResourcesByCourseId(Long courseId) {
        return courseResourceDao.countByCourseId(courseId);
    }

    @Override
    public ResourceVO getResourceById(Long resourceId) {
        return resourceConverter.toVO(resourceDao.findById(resourceId));
    }

    @Override
    public List<ResourceVO> listResourcesByIds(List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return resourceDao.findByIds(resourceIds).stream()
                .map(resourceConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceVO> listResourcesByCourse(Long courseId, Long chapterId, Integer limit) {
        return resourceDao.findByCourseAndChapter(courseId, chapterId, limit).stream()
                .map(resourceConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public String getResourceDownloadUrl(Long resourceId) {
        ResourceEntity entity = resourceDao.findById(resourceId);
        return entity != null ? entity.getFileUrl() : null;
    }
}
