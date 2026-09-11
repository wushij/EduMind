package com.edumind.question.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.question.entity.QuestionBankEntity;
import com.edumind.question.mapper.QuestionBankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class QuestionBankDao {

    private final QuestionBankMapper questionBankMapper;

    public QuestionBankEntity getById(Long id) {
        return questionBankMapper.selectById(id);
    }

    public Page<QuestionBankEntity> pageQuery(Long courseId, String keyword, long pageNum, long pageSize) {
        Page<QuestionBankEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<QuestionBankEntity> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(QuestionBankEntity::getCourseId, courseId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(QuestionBankEntity::getName, keyword);
        }
        wrapper.orderByDesc(QuestionBankEntity::getUpdateTime);
        return questionBankMapper.selectPage(page, wrapper);
    }

    public int insert(QuestionBankEntity entity) {
        return questionBankMapper.insert(entity);
    }

    public int updateById(QuestionBankEntity entity) {
        return questionBankMapper.updateById(entity);
    }

    public int softDeleteById(Long id) {
        return questionBankMapper.deleteById(id);
    }
}
