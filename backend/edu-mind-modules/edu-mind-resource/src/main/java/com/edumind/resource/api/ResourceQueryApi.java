package com.edumind.resource.api;

import java.util.List;

/**
 * 资源领域跨模块只读查询公开 API
 * 供 Course（挂载附件）、Knowledge（提取文档构建知识库）、AI 模块跨域调用
 */
public interface ResourceQueryApi {

    Object getResourceById(Long resourceId);

    List<?> listResourcesByIds(List<Long> resourceIds);

    List<?> listResourcesByCourse(Long courseId, Long chapterId, Integer limit);

    String getResourceDownloadUrl(Long resourceId);
}
