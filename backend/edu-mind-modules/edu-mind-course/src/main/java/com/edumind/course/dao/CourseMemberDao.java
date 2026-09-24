package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.course.entity.CourseMemberEntity;
import com.edumind.course.mapper.CourseMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CourseMemberDao {

    private final CourseMemberMapper courseMemberMapper;

    public List<CourseMemberEntity> findByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return courseMemberMapper.selectList(new LambdaQueryWrapper<CourseMemberEntity>()
                .eq(CourseMemberEntity::getCourseId, courseId)
                .orderByAsc(CourseMemberEntity::getCreateTime));
    }

    public List<Long> findCourseIdsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return courseMemberMapper.selectList(new LambdaQueryWrapper<CourseMemberEntity>()
                        .eq(CourseMemberEntity::getUserId, userId))
                .stream()
                .map(CourseMemberEntity::getCourseId)
                .distinct()
                .collect(Collectors.toList());
    }

    public long countStudentsByCourseId(Long courseId) {
        if (courseId == null) {
            return 0L;
        }
        return courseMemberMapper.selectCount(new LambdaQueryWrapper<CourseMemberEntity>()
                .eq(CourseMemberEntity::getCourseId, courseId)
                .eq(CourseMemberEntity::getMemberRole, "STUDENT"));
    }

    public CourseMemberEntity findByCourseIdAndUserId(Long courseId, Long userId) {
        return courseMemberMapper.selectOne(new LambdaQueryWrapper<CourseMemberEntity>()
                .eq(CourseMemberEntity::getCourseId, courseId)
                .eq(CourseMemberEntity::getUserId, userId));
    }

    public List<Long> findAllUserIdsByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return courseMemberMapper.selectList(new LambdaQueryWrapper<CourseMemberEntity>()
                .eq(CourseMemberEntity::getCourseId, courseId))
                .stream()
                .map(CourseMemberEntity::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Long> findStudentUserIdsByCourseId(Long courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return courseMemberMapper.selectList(new LambdaQueryWrapper<CourseMemberEntity>()
                .eq(CourseMemberEntity::getCourseId, courseId)
                .eq(CourseMemberEntity::getMemberRole, "STUDENT"))
                .stream()
                .map(CourseMemberEntity::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public int insert(CourseMemberEntity entity) {
        return courseMemberMapper.insert(entity);
    }

    public int deleteByCourseIdAndUserId(Long courseId, Long userId) {
        if (courseId == null || userId == null) {
            return 0;
        }
        return courseMemberMapper.delete(new LambdaQueryWrapper<CourseMemberEntity>()
                .eq(CourseMemberEntity::getCourseId, courseId)
                .eq(CourseMemberEntity::getUserId, userId));
    }

    public int deleteByCourseId(Long courseId) {
        if (courseId == null) {
            return 0;
        }
        return courseMemberMapper.delete(new LambdaQueryWrapper<CourseMemberEntity>()
                .eq(CourseMemberEntity::getCourseId, courseId));
    }
}
