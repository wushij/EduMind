package com.edumind.resource.api;

/**
 * 资源领域跨模块写操作公开 API
 * 供 Course 等业务领域在课程删除时进行级联资源清理
 */
public interface ResourceCommandApi {

    /**
     * 删除指定课程下的全部资源关联与数据
     *
     * @param courseId 课程 ID
     */
    void deleteResourcesByCourseId(Long courseId);
}
