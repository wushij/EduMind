package com.edumind.course.service.knowledge;

import com.edumind.course.dto.knowledge.KnowledgePointCreateDTO;
import com.edumind.course.dto.knowledge.KnowledgePointUpdateDTO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;

import java.util.List;

public interface KnowledgePointService {

    List<KnowledgePointVO> listByCourse(Long courseId, Long chapterId);

    KnowledgePointVO getById(Long courseId, Long kpId);

    KnowledgePointVO create(Long courseId, KnowledgePointCreateDTO dto);

    KnowledgePointVO update(Long courseId, Long kpId, KnowledgePointUpdateDTO dto);

    void delete(Long courseId, Long kpId);
}
