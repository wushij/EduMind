package com.edumind.resource.service;

import com.edumind.resource.vo.ResourceVO;

import java.util.List;

public interface ResourceQueryService {

    Long countResourcesByCourseId(Long courseId);

    ResourceVO getResourceById(Long resourceId);

    List<ResourceVO> listResourcesByIds(List<Long> resourceIds);

    List<ResourceVO> listResourcesByCourse(Long courseId, Long chapterId, Integer limit);

    String getResourceDownloadUrl(Long resourceId);
}
