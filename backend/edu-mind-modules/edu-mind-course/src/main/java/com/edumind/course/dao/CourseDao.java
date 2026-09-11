package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.course.dto.course.CourseQueryDTO;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseDao {

    private final CourseMapper courseMapper;

    public CourseEntity findById(Long id) {
        return courseMapper.selectById(id);
    }

    public List<CourseEntity> findByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return courseMapper.selectBatchIds(ids);
    }

    public List<CourseEntity> findByTeacherId(Long teacherId) {
        if (teacherId == null) {
            return Collections.emptyList();
        }
        return courseMapper.selectList(new LambdaQueryWrapper<CourseEntity>()
                .eq(CourseEntity::getTeacherId, teacherId)
                .orderByDesc(CourseEntity::getCreateTime));
    }

    public Page<CourseEntity> pageQuery(CourseQueryDTO query, List<Long> courseIds) {
        long pageNum = query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1L;
        long pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10L;
        Page<CourseEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CourseEntity> wrapper = new LambdaQueryWrapper<>();
        if (!CollectionUtils.isEmpty(courseIds)) {
            wrapper.in(CourseEntity::getId, courseIds);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(CourseEntity::getTitle, query.getKeyword())
                    .or()
                    .like(CourseEntity::getCode, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            if ("ACTIVE".equalsIgnoreCase(query.getStatus())) {
                wrapper.eq(CourseEntity::getStatus, 1);
            } else if ("INACTIVE".equalsIgnoreCase(query.getStatus())) {
                wrapper.ne(CourseEntity::getStatus, 1);
            }
        }
        wrapper.orderByDesc(CourseEntity::getCreateTime);
        return courseMapper.selectPage(page, wrapper);
    }

    public Page<CourseEntity> pageQueryAll(CourseQueryDTO query) {
        return pageQuery(query, null);
    }

    public int insert(CourseEntity entity) {
        return courseMapper.insert(entity);
    }

    public int updateById(CourseEntity entity) {
        return courseMapper.updateById(entity);
    }

    public long countAll() {
        return courseMapper.selectCount(null);
    }

    public List<CourseEntity> findRecent(int limit) {
        int size = limit > 0 ? limit : 5;
        return courseMapper.selectList(new LambdaQueryWrapper<CourseEntity>()
                .orderByDesc(CourseEntity::getUpdateTime)
                .last("LIMIT " + size));
    }
}
