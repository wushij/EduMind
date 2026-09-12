package com.edumind.statistics.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.mapper.WrongQuestionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WrongQuestionRecordDao {

    private final WrongQuestionRecordMapper wrongQuestionRecordMapper;

    public Page<WrongQuestionRecordEntity> pageByCourse(Page<WrongQuestionRecordEntity> page,
                                                        Long courseId, Long knowledgePointId) {
        LambdaQueryWrapper<WrongQuestionRecordEntity> wrapper = new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                .eq(knowledgePointId != null, WrongQuestionRecordEntity::getKnowledgePointId, knowledgePointId)
                .orderByDesc(WrongQuestionRecordEntity::getWrongCount);
        return wrongQuestionRecordMapper.selectPage(page, wrapper);
    }

    public int insert(WrongQuestionRecordEntity entity) {
        return wrongQuestionRecordMapper.insert(entity);
    }

    public int updateById(WrongQuestionRecordEntity entity) {
        return wrongQuestionRecordMapper.updateById(entity);
    }

    public WrongQuestionRecordEntity findById(Long id) {
        return wrongQuestionRecordMapper.selectById(id);
    }

    public WrongQuestionRecordEntity findByStudentAndQuestion(Long studentId, Long questionId) {
        return wrongQuestionRecordMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getQuestionId, questionId)
                        .last("LIMIT 1")
        );
    }
}
