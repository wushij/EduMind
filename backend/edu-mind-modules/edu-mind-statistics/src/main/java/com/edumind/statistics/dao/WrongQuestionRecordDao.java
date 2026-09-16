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

    public Page<WrongQuestionRecordEntity> pageByStudent(Page<WrongQuestionRecordEntity> page,
                                                         Long studentId, Long courseId) {
        LambdaQueryWrapper<WrongQuestionRecordEntity> wrapper = new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                .eq(WrongQuestionRecordEntity::getCourseId, courseId)
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

    public WrongQuestionRecordEntity findByStudentCourseAndKp(Long studentId, Long courseId, Long knowledgePointId) {
        return wrongQuestionRecordMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .eq(WrongQuestionRecordEntity::getKnowledgePointId, knowledgePointId)
                        .orderByDesc(WrongQuestionRecordEntity::getWrongCount)
                        .last("LIMIT 1")
        );
    }

    public long countByCourse(Long courseId) {
        return wrongQuestionRecordMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
        );
    }

    public long countByCourseAndDateRange(Long courseId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return wrongQuestionRecordMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .ge(start != null, WrongQuestionRecordEntity::getCreateTime, start)
                        .le(end != null, WrongQuestionRecordEntity::getCreateTime, end)
        );
    }

    public long countByStudentAndCourse(Long studentId, Long courseId) {
        return wrongQuestionRecordMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
        );
    }

    public java.util.List<WrongQuestionRecordEntity> listByStudentAndCourse(Long studentId, Long courseId, int limit) {
        return wrongQuestionRecordMapper.selectList(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .orderByDesc(WrongQuestionRecordEntity::getWrongCount)
                        .last(limit > 0 ? "LIMIT " + limit : "")
        );
    }
}
