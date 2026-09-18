package com.edumind.course.api.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseKnowledgeBaseCommandApi;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.CourseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseKnowledgeBaseCommandApiImpl implements CourseKnowledgeBaseCommandApi {

    private final CourseDao courseDao;

    @Override
    public void bindKnowledgeBaseId(Long courseId, Long knowledgeBaseId) {
        if (courseId == null || knowledgeBaseId == null) {
            throw new BusinessException("绑定知识库参数不完整");
        }
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        course.setKnowledgeBaseId(knowledgeBaseId);
        courseDao.updateById(course);
    }
}
