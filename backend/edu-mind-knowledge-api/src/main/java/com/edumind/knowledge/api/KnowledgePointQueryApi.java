package com.edumind.knowledge.api;

import com.edumind.course.vo.knowledge.KnowledgePointVO;

import java.util.List;

/**
 * 知识点跨模块只读查询公开 API（数据委托 course 模块）
 */
public interface KnowledgePointQueryApi {

    List<KnowledgePointVO> listByCourseId(Long courseId);

    KnowledgePointVO getById(Long id);
}
