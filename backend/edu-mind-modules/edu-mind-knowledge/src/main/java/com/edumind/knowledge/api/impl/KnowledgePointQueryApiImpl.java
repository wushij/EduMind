package com.edumind.knowledge.api.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgePointQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgePointQueryApiImpl implements KnowledgePointQueryApi {

    private final CourseQueryApi courseQueryApi;

    @Override
    public List<KnowledgePointVO> listByCourseId(Long courseId) {
        return courseQueryApi.listKnowledgePointsByCourseId(courseId);
    }

    @Override
    public KnowledgePointVO getById(Long id) {
        return courseQueryApi.getKnowledgePointById(id);
    }
}
