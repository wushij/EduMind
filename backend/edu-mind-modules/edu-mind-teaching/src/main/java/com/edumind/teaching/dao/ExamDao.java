package com.edumind.teaching.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.teaching.entity.ExamEntity;
import com.edumind.teaching.mapper.ExamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ExamDao {

    private final ExamMapper examMapper;

    public ExamEntity findById(Long id) {
        return examMapper.selectById(id);
    }

    public List<ExamEntity> findByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return examMapper.selectList(
                new LambdaQueryWrapper<ExamEntity>()
                        .eq(ExamEntity::getCourseId, courseId)
                        .orderByDesc(ExamEntity::getCreateTime)
        );
    }

    public Page<ExamEntity> pageQuery(Long courseId, String keyword, long pageNum, long pageSize) {
        Page<ExamEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExamEntity> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(ExamEntity::getCourseId, courseId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ExamEntity::getTitle, keyword);
        }
        wrapper.orderByDesc(ExamEntity::getCreateTime);
        return examMapper.selectPage(page, wrapper);
    }

    public int insert(ExamEntity entity) {
        return examMapper.insert(entity);
    }

    public int updateById(ExamEntity entity) {
        return examMapper.updateById(entity);
    }

    public int softDeleteById(Long id) {
        return examMapper.deleteById(id);
    }

    public long countAll() {
        return examMapper.selectCount(new LambdaQueryWrapper<>());
    }
}
