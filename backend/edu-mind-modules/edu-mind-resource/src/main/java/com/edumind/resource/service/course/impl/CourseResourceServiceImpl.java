package com.edumind.resource.service.course.impl;

import com.edumind.resource.dao.CourseResourceDao;
import com.edumind.resource.entity.CourseResourceEntity;
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

    @Override
    public List<CourseResourceVO> listByCourseId(Long courseId) {
        return courseResourceDao.findByCourseId(courseId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long addResource(Long courseId, com.edumind.resource.dto.course.CourseResourceCreateDTO dto) {
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
        return vo;
    }
}
