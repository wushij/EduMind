package com.edumind.course.api;

/**
 * 跨模块课程访问校验（按课程维度，非全局 RBAC）。
 */
public interface CourseAccessApi {

    void assertCanView(Long courseId);

    void assertCanEdit(Long courseId);
}
