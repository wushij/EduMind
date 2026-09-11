package com.edumind.knowledge.service.knowledge;

import com.edumind.knowledge.dto.knowledge.KnowledgeBaseCreateDTO;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseUpdateDTO;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;

import java.util.List;

public interface KnowledgeBaseService {

    Long create(KnowledgeBaseCreateDTO dto);

    KnowledgeBaseVO getById(Long id);

    List<KnowledgeBaseVO> list(Long courseId);

    void update(Long id, KnowledgeBaseUpdateDTO dto);

    void delete(Long id);
}
