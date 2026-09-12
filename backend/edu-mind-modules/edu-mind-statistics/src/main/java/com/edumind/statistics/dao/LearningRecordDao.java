package com.edumind.statistics.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.statistics.entity.LearningRecordEntity;
import com.edumind.statistics.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LearningRecordDao {

    private final LearningRecordMapper learningRecordMapper;

    public List<LearningRecordEntity> listByCourseSince(Long courseId, LocalDateTime since) {
        return learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecordEntity>()
                        .eq(LearningRecordEntity::getCourseId, courseId)
                        .ge(since != null, LearningRecordEntity::getCreateTime, since)
        );
    }

    public int insert(LearningRecordEntity entity) {
        return learningRecordMapper.insert(entity);
    }
}
