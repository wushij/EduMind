package com.edumind.course.api;

/**
 * 跨模块课程访问校验（按课程维度，非全局 RBAC）。
 */
public interface CourseAccessApi {

    void assertCanView(Long courseId);

    void assertCanEdit(Long courseId);

    /**
     * 解析当前登录用户的课程可见范围。
     *
     * <p>与课程中心列表（{@code GET /api/courses}）使用同一套规则：租户数据范围、
     * 教师任职课程、学生选课课程、院系管理员组织子树课程。任何「按课程归属收敛数据」
     * 的跨模块查询都应先取本模型，再在 SQL 中以 {@code course_id IN (...)} 落地过滤，
     * 严禁只校验权限码就把全量数据返回给调用方。</p>
     */
    CourseDataScope resolveCurrentDataScope();

    /**
     * 当前登录用户是否可访问指定课程（口径与 {@link #resolveCurrentDataScope()} 完全一致）。
     * 用于「列表已按范围过滤、单条读取需复核」的场景，避免直接按 id 遍历越权读取。
     */
    boolean isCourseVisible(Long courseId);
}
