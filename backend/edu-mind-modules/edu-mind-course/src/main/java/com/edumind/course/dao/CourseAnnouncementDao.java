package com.edumind.course.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.course.entity.CourseAnnouncementEntity;
import com.edumind.course.mapper.CourseAnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseAnnouncementDao {

    private final CourseAnnouncementMapper mapper;

    public CourseAnnouncementEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public CourseAnnouncementEntity findByIdAndCourseId(Long id, Long courseId) {
        return mapper.selectOne(new LambdaQueryWrapper<CourseAnnouncementEntity>()
                .eq(CourseAnnouncementEntity::getId, id)
                .eq(CourseAnnouncementEntity::getCourseId, courseId));
    }

    public long countPublishedByCourseId(Long courseId) {
        return mapper.selectCount(new LambdaQueryWrapper<CourseAnnouncementEntity>()
                .eq(CourseAnnouncementEntity::getCourseId, courseId)
                .eq(CourseAnnouncementEntity::getStatus, "PUBLISHED"));
    }

    public List<CourseAnnouncementEntity> listPreview(Long courseId, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<CourseAnnouncementEntity>()
                .eq(CourseAnnouncementEntity::getCourseId, courseId)
                .eq(CourseAnnouncementEntity::getStatus, "PUBLISHED")
                .orderByDesc(CourseAnnouncementEntity::getPinned)
                .orderByDesc(CourseAnnouncementEntity::getPublishTime)
                .last("LIMIT " + limit));
    }

    public PageResult<CourseAnnouncementEntity> pageByCourseId(Long courseId, long pageNum, long pageSize, String status) {
        LambdaQueryWrapper<CourseAnnouncementEntity> wrapper = new LambdaQueryWrapper<CourseAnnouncementEntity>()
                .eq(CourseAnnouncementEntity::getCourseId, courseId)
                .orderByDesc(CourseAnnouncementEntity::getPinned)
                .orderByDesc(CourseAnnouncementEntity::getPublishTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(CourseAnnouncementEntity::getStatus, status);
        }
        Page<CourseAnnouncementEntity> page = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.<CourseAnnouncementEntity>builder()
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .list(page.getRecords())
                .build();
    }

    public int insert(CourseAnnouncementEntity entity) {
        return mapper.insert(entity);
    }

    public int updateById(CourseAnnouncementEntity entity) {
        return mapper.updateById(entity);
    }
}
