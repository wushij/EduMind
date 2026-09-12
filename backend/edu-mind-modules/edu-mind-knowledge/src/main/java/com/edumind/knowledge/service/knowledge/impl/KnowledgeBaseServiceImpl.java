package com.edumind.knowledge.service.knowledge.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.knowledge.converter.KnowledgeBaseConverter;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseCreateDTO;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseUpdateDTO;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.service.knowledge.KnowledgeBaseService;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeBaseConverter knowledgeBaseConverter;
    private final KnowledgeAccessService knowledgeAccessService;

    @Override
    public Long create(KnowledgeBaseCreateDTO dto) {
        KnowledgeBaseEntity entity = knowledgeBaseConverter.toEntity(dto);
        knowledgeBaseDao.insert(entity);
        return entity.getId();
    }

    @Override
    public KnowledgeBaseVO getById(Long id) {
        KnowledgeBaseEntity entity = knowledgeAccessService.assertAccessible(id);
        return knowledgeBaseConverter.toVO(entity);
    }

    @Override
    public List<KnowledgeBaseVO> list(Long courseId) {
        List<KnowledgeBaseEntity> entities = knowledgeAccessService.listAccessibleKnowledgeBases(courseId);
        return entities.stream().map(knowledgeBaseConverter::toVO).collect(Collectors.toList());
    }

    @Override
    public void update(Long id, KnowledgeBaseUpdateDTO dto) {
        KnowledgeBaseEntity entity = knowledgeAccessService.assertAccessible(id);
        if (entity == null) {
            throw new BusinessException("知识库不存在");
        }
        knowledgeBaseConverter.applyUpdate(entity, dto);
        knowledgeBaseDao.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        knowledgeAccessService.assertAccessible(id);
        knowledgeBaseDao.deleteById(id);
    }
}
