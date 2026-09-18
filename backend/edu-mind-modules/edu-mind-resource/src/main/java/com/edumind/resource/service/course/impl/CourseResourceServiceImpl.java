package com.edumind.resource.service.course.impl;

import com.edumind.course.api.CourseAccessApi;
import com.edumind.resource.dao.CourseResourceDao;
import com.edumind.resource.dao.ResourceDao;
import com.edumind.resource.entity.CourseResourceEntity;
import com.edumind.resource.entity.ResourceEntity;
import com.edumind.resource.service.course.CourseResourceService;
import com.edumind.resource.vo.CourseResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseResourceServiceImpl implements CourseResourceService {

    private final CourseResourceDao courseResourceDao;
    private final ResourceDao resourceDao;
    private final CourseAccessApi courseAccessApi;

    @Override
    public List<CourseResourceVO> listByCourseId(Long courseId) {
        return listByCourseId(courseId, null);
    }

    @Override
    public List<CourseResourceVO> listByCourseId(Long courseId, Long chapterId) {
        courseAccessApi.assertCanView(courseId);
        if (chapterId != null) {
            return resourceDao.findByCourseAndChapter(courseId, chapterId, null).stream()
                    .map(this::teachingToVO)
                    .collect(Collectors.toList());
        }
        return courseResourceDao.findByCourseId(courseId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long addResource(Long courseId, com.edumind.resource.dto.course.CourseResourceCreateDTO dto) {
        courseAccessApi.assertCanEdit(courseId);
        CourseResourceEntity entity = new CourseResourceEntity();
        entity.setCourseId(courseId);
        entity.setResourceId(dto.getResourceId());
        entity.setDocumentId(dto.getDocumentId());
        entity.setTitle(dto.getTitle().trim());
        String type = dto.getResourceType();
        if (type == null || type.isBlank()) {
            String lower = dto.getTitle().toLowerCase();
            if (lower.endsWith(".pdf")) type = "PDF";
            else if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) type = "PPT";
            else if (lower.endsWith(".doc") || lower.endsWith(".docx")) type = "WORD";
            else if (lower.endsWith(".mp4") || lower.endsWith(".avi")) type = "VIDEO";
            else type = "DOCUMENT";
        }
        entity.setResourceType(type.toUpperCase());
        entity.setCreateTime(java.time.LocalDateTime.now());
        courseResourceDao.insert(entity);
        return entity.getId();
    }

    @Override
    public void deleteResource(Long resourceId) {
        CourseResourceEntity existing = courseResourceDao.findById(resourceId);
        if (existing != null) {
            courseAccessApi.assertCanEdit(existing.getCourseId());
        }
        courseResourceDao.deleteById(resourceId);
    }

    private CourseResourceVO toVO(CourseResourceEntity entity) {
        CourseResourceVO vo = new CourseResourceVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setResourceId(entity.getResourceId());
        vo.setDocumentId(entity.getDocumentId());
        vo.setTitle(entity.getTitle());
        vo.setResourceType(entity.getResourceType());
        vo.setCreateTime(entity.getCreateTime());
        if (entity.getResourceId() != null) {
            ResourceEntity resource = resourceDao.findById(entity.getResourceId());
            if (resource != null) {
                vo.setChapterId(resource.getChapterId());
                vo.setDownloadUrl(resource.getFileUrl());
            }
        }
        return vo;
    }

    private CourseResourceVO teachingToVO(ResourceEntity entity) {
        CourseResourceVO vo = new CourseResourceVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setResourceId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setResourceType(entity.getResourceType());
        vo.setChapterId(entity.getChapterId());
        vo.setDownloadUrl(entity.getFileUrl());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
