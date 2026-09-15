package com.edumind.resource.api.impl;

import com.edumind.resource.api.ResourceQueryApi;
import com.edumind.resource.service.ResourceQueryService;
import com.edumind.resource.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceQueryApiImpl implements ResourceQueryApi {

    private final ResourceQueryService resourceQueryService;

    @Override
    public Long countResourcesByCourseId(Long courseId) {
        return resourceQueryService.countResourcesByCourseId(courseId);
    }

    @Override
    public ResourceVO getResourceById(Long resourceId) {
        return resourceQueryService.getResourceById(resourceId);
    }

    @Override
    public List<ResourceVO> listResourcesByIds(List<Long> resourceIds) {
        return resourceQueryService.listResourcesByIds(resourceIds);
    }

    @Override
    public List<ResourceVO> listResourcesByCourse(Long courseId, Long chapterId, Integer limit) {
        return resourceQueryService.listResourcesByCourse(courseId, chapterId, limit);
    }

    @Override
    public String getResourceDownloadUrl(Long resourceId) {
        return resourceQueryService.getResourceDownloadUrl(resourceId);
    }
}
