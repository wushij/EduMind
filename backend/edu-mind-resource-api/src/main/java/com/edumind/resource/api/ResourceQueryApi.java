package com.edumind.resource.api;

import com.edumind.resource.vo.ResourceVO;

import java.util.List;

/**
 * 资源领域跨模块只读查询公开 API
 * 供 Course（挂载附件）、Knowledge（提取文档构建知识库）、AI 模块跨域调用
 */
public interface ResourceQueryApi {

    ResourceVO getResourceById(Long resourceId);

    List<ResourceVO> listResourcesByIds(List<Long> resourceIds);

    List<ResourceVO> listResourcesByCourse(Long courseId, Long chapterId, Integer limit);

    String getResourceDownloadUrl(Long resourceId);

    Long countResourcesByCourseId(Long courseId);
}
