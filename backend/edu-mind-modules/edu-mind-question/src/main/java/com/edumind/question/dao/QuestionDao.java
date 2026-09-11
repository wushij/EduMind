package com.edumind.question.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.question.dto.question.QuestionQueryDTO;
import com.edumind.question.entity.QuestionEntity;
import com.edumind.question.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class QuestionDao {

    private final QuestionMapper questionMapper;

    public QuestionEntity getById(Long id) {
        return questionMapper.selectById(id);
    }

    public List<QuestionEntity> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return questionMapper.selectBatchIds(ids);
    }

    public List<QuestionEntity> listByKnowledgePointId(Long pointId, Integer limit) {
        return questionMapper.selectList(
                new LambdaQueryWrapper<QuestionEntity>()
                        .eq(QuestionEntity::getKnowledgePointId, pointId)
                        .last(limit != null && limit > 0, "LIMIT " + limit)
        );
    }

    public List<QuestionEntity> listByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return questionMapper.selectList(
                new LambdaQueryWrapper<QuestionEntity>()
                        .eq(QuestionEntity::getCourseId, courseId)
                        .orderByDesc(QuestionEntity::getCreateTime)
        );
    }

    public List<QuestionEntity> listByCourseAndTypes(Long courseId, List<String> types, Integer limit) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<QuestionEntity> wrapper = new LambdaQueryWrapper<QuestionEntity>()
                .eq(QuestionEntity::getCourseId, courseId)
                .in(types != null && !types.isEmpty(), QuestionEntity::getType, types)
                .orderByDesc(QuestionEntity::getCreateTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return questionMapper.selectList(wrapper);
    }

    public Page<QuestionEntity> pageQuery(QuestionQueryDTO query) {
        long pageNum = query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1L;
        long pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10L;
        Page<QuestionEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<QuestionEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getCourseId() != null) {
            wrapper.eq(QuestionEntity::getCourseId, query.getCourseId());
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(QuestionEntity::getType, query.getType());
        }
        if (query.getDifficulty() != null) {
            wrapper.eq(QuestionEntity::getDifficulty, query.getDifficulty());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(QuestionEntity::getStem, query.getKeyword());
        }
        wrapper.orderByDesc(QuestionEntity::getCreateTime);
        return questionMapper.selectPage(page, wrapper);
    }

    public int insert(QuestionEntity entity) {
        return questionMapper.insert(entity);
    }

    public int updateById(QuestionEntity entity) {
        return questionMapper.updateById(entity);
    }

    public int softDeleteById(Long id) {
        return questionMapper.deleteById(id);
    }

    public long countAll() {
        return questionMapper.selectCount(null);
    }
}
