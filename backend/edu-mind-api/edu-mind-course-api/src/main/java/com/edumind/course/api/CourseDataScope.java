package com.edumind.course.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 课程级数据范围模型：描述当前登录用户在「课程维度」上的可见范围。
 *
 * <p>与 {@link com.edumind.system.api.TenantDataScope}（租户级五级数据范围）不同，
 * 本模型是课程模块对外收敛后的结果：租户数据范围 + 教师任职课程 + 学生选课课程 +
 * 院系管理员组织子树课程，统一折算成一份课程 ID 集合。</p>
 *
 * <p>存在的意义：作业、考试、学情统计等模块都需要「这个人能看到哪些课程」这一个判断，
 * 若各模块自行拼装过滤条件，就会出现课程中心按范围过滤、而作业列表全量返回的越权问题。
 * 因此凡是按课程归属收敛数据的查询，都必须先取本模型再落到 SQL。</p>
 */
public class CourseDataScope implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否不限课程（平台超管 / 租户管理员，等价于租户内全量） */
    private final boolean all;

    /** 可见课程 ID 集合；{@link #all} 为 true 时该集合恒为空且无业务含义 */
    private final Set<Long> courseIds;

    private CourseDataScope(boolean all, Set<Long> courseIds) {
        this.all = all;
        this.courseIds = courseIds;
    }

    /** 租户内全量可见（不做课程过滤） */
    public static CourseDataScope all() {
        return new CourseDataScope(true, Collections.emptySet());
    }

    /**
     * 指定课程集合可见。
     *
     * @param courseIds 可见课程 ID；传 null 或空集合表示「无任何可见课程」，
     *                  调用方必须直接返回空结果，严禁把空集合当成「不过滤」
     */
    public static CourseDataScope of(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return new CourseDataScope(false, Collections.emptySet());
        }
        Set<Long> distinct = new HashSet<>();
        for (Long id : courseIds) {
            if (id != null) {
                distinct.add(id);
            }
        }
        return new CourseDataScope(false, Collections.unmodifiableSet(distinct));
    }

    public boolean isAll() {
        return all;
    }

    public Set<Long> getCourseIds() {
        return courseIds;
    }

    /** 是否存在「无任何可见课程」——为 true 时必须短路返回空结果 */
    public boolean isEmpty() {
        return !all && courseIds.isEmpty();
    }

    /** 指定课程是否在可见范围内 */
    public boolean contains(Long courseId) {
        if (courseId == null) {
            return false;
        }
        return all || courseIds.contains(courseId);
    }
}
