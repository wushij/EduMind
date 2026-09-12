package com.edumind.statistics.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.mapper.KnowledgeMasteryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeMasteryDao {

    private final KnowledgeMasteryMapper knowledgeMasteryMapper;

    public List<KnowledgeMasteryEntity> listByCourse(Long courseId) {
        return knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMasteryEntity>()
                        .eq(KnowledgeMasteryEntity::getCourseId, courseId)
        );
    }

    public List<KnowledgeMasteryEntity> listByCourseAndStudent(Long courseId, Long studentId) {
        return knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMasteryEntity>()
                        .eq(KnowledgeMasteryEntity::getCourseId, courseId)
                        .eq(KnowledgeMasteryEntity::getStudentId, studentId)
        );
    }

    public KnowledgeMasteryEntity findByStudentAndKp(Long studentId, Long knowledgePointId) {
        return knowledgeMasteryMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeMasteryEntity>()
                        .eq(KnowledgeMasteryEntity::getStudentId, studentId)
                        .eq(KnowledgeMasteryEntity::getKnowledgePointId, knowledgePointId)
        );
    }

    public int insert(KnowledgeMasteryEntity entity) {
        return knowledgeMasteryMapper.insert(entity);
    }

    public int updateById(KnowledgeMasteryEntity entity) {
        return knowledgeMasteryMapper.updateById(entity);
    }
}
