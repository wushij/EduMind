package com.edumind.teaching.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.mapper.AssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AssignmentDao {

    private final AssignmentMapper assignmentMapper;

    public AssignmentEntity findById(Long id) {
        return assignmentMapper.selectById(id);
    }

    /**
     * 作业分页查询。
     *
     * @param courseId          页面显式指定的课程过滤，可为 null
     * @param visibleCourseIds  当前用户可见课程集合；<b>null 表示不限</b>（仅平台/租户管理员），
     *                          空集合表示无任何可见课程，将直接返回空页。
     *                          该参数是防止「越权看到他人课程作业」的关键：调用方必须先经
     *                          {@code CourseAccessApi.resolveCurrentDataScope()} 收敛，禁止传 null 表示"不过滤"。
     * @param status            作业状态过滤，可为 null
     * @param keyword           标题模糊搜索，可为 null
     */
    public Page<AssignmentEntity> pageQuery(Long courseId, Collection<Long> visibleCourseIds, String status,
                                            String keyword, long pageNum, long pageSize) {
        Page<AssignmentEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssignmentEntity> wrapper = new LambdaQueryWrapper<>();
        if (visibleCourseIds != null) {
            if (visibleCourseIds.isEmpty()) {
                // 无可见课程：务必短路返回空页，否则会退化成全表查询
                return page;
            }
            wrapper.in(AssignmentEntity::getCourseId, visibleCourseIds);
        }
        if (courseId != null) {
            wrapper.eq(AssignmentEntity::getCourseId, courseId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AssignmentEntity::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AssignmentEntity::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(AssignmentEntity::getCreateTime);
        return assignmentMapper.selectPage(page, wrapper);
    }

    /** 指定课程集合下的全部作业（用于答卷总览等按课程收敛的批量查询） */
    public List<AssignmentEntity> listByCourseIds(Collection<Long> courseIds) {
        if (courseIds == null) {
            return assignmentMapper.selectList(new LambdaQueryWrapper<>());
        }
        if (courseIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return assignmentMapper.selectList(
                new LambdaQueryWrapper<AssignmentEntity>().in(AssignmentEntity::getCourseId, courseIds)
        );
    }

    public List<AssignmentEntity> listPublishedByCourseIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return assignmentMapper.selectList(
                new LambdaQueryWrapper<AssignmentEntity>()
                        .in(AssignmentEntity::getCourseId, courseIds)
                        .eq(AssignmentEntity::getStatus, "PUBLISHED")
                        .orderByDesc(AssignmentEntity::getDeadline)
        );
    }

    public int insert(AssignmentEntity entity) {
        return assignmentMapper.insert(entity);
    }

    public int updateById(AssignmentEntity entity) {
        return assignmentMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return assignmentMapper.deleteById(id);
    }

    public long countAll() {
        return assignmentMapper.selectCount(new LambdaQueryWrapper<>());
    }

    public long countByStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return 0L;
        }
        return assignmentMapper.selectCount(
                new LambdaQueryWrapper<AssignmentEntity>().eq(AssignmentEntity::getStatus, status)
        );
    }

    public java.util.List<AssignmentEntity> listByCourseId(Long courseId) {
        if (courseId == null) {
            return java.util.Collections.emptyList();
        }
        return assignmentMapper.selectList(
                new LambdaQueryWrapper<AssignmentEntity>().eq(AssignmentEntity::getCourseId, courseId)
        );
    }
}
