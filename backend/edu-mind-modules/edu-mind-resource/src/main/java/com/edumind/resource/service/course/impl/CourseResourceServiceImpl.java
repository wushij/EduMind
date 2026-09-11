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
