package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.mapper.ChapterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChapterDao {

    private final ChapterMapper chapterMapper;

    public List<ChapterEntity> findByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return chapterMapper.selectList(new LambdaQueryWrapper<ChapterEntity>()
                .eq(ChapterEntity::getCourseId, courseId)
                .orderByAsc(ChapterEntity::getSortOrder)
                .orderByAsc(ChapterEntity::getId));
    }
}
