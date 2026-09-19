package com.edumind.teaching.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.mapper.AssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AssignmentDao {

    private final AssignmentMapper assignmentMapper;

    public AssignmentEntity findById(Long id) {
        return assignmentMapper.selectById(id);
    }

    public Page<AssignmentEntity> pageQuery(Long courseId, String status, String keyword, long pageNum, long pageSize) {
        Page<AssignmentEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssignmentEntity> wrapper = new LambdaQueryWrapper<>();
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
