package com.edumind.ai.service.prompt.impl;

import com.edumind.ai.dao.PromptTemplateDao;
import com.edumind.ai.dao.PromptTemplateVersionDao;
import com.edumind.ai.dto.prompt.PromptTemplateDTO;
import com.edumind.ai.dto.prompt.PromptTestDTO;
import com.edumind.ai.entity.PromptTemplateEntity;
import com.edumind.ai.entity.PromptTemplateVersionEntity;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.service.prompt.PromptManageService;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.vo.prompt.PromptTemplateVO;
import com.edumind.ai.vo.prompt.PromptTemplateVersionVO;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromptManageServiceImpl implements PromptManageService {

    private final PromptTemplateDao promptTemplateDao;
    private final PromptTemplateVersionDao promptTemplateVersionDao;
    private final PromptService promptService;
    private final LlmClient llmClient;

    @Override
    public List<PromptTemplateVO> list(String category, String status, String keyword) {
        return promptTemplateDao.list(category, status, keyword).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public PromptTemplateVO getById(Long id) {
        PromptTemplateEntity entity = promptTemplateDao.findById(id);
        if (entity == null) {
            throw new BusinessException("模板不存在");
        }
        return toVO(entity);
    }

    @Override
    public List<PromptTemplateVersionVO> listVersions(Long templateId) {
        return promptTemplateVersionDao.listByTemplateId(templateId).stream()
                .map(this::toVersionVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PromptTemplateDTO dto) {
        if (promptTemplateDao.findByCode(dto.getCode()) != null) {
            throw new BusinessException("模板编码已存在");
        }
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setContent(dto.getContent());
        entity.setVariables(dto.getVariables());
        entity.setStatus("DRAFT");
        entity.setVersion(1);
        promptTemplateDao.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, PromptTemplateDTO dto) {
        PromptTemplateEntity entity = promptTemplateDao.findById(id);
        if (entity == null) {
            throw new BusinessException("模板不存在");
        }
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getCategory() != null) {
            entity.setCategory(dto.getCategory());
        }
        if (dto.getContent() != null) {
            entity.setContent(dto.getContent());
        }
        if (dto.getVariables() != null) {
            entity.setVariables(dto.getVariables());
        }
        entity.setStatus("DRAFT");
        promptTemplateDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        PromptTemplateEntity entity = promptTemplateDao.findById(id);
        if (entity == null) {
            throw new BusinessException("模板不存在");
        }
        int nextVersion = entity.getVersion() != null ? entity.getVersion() + 1 : 1;
        entity.setStatus("PUBLISHED");
        entity.setVersion(nextVersion);
        promptTemplateDao.updateById(entity);

        PromptTemplateVersionEntity versionEntity = new PromptTemplateVersionEntity();
        versionEntity.setTemplateId(entity.getId());
        versionEntity.setVersion(nextVersion);
        versionEntity.setContent(entity.getContent());
        versionEntity.setVariables(entity.getVariables());
        versionEntity.setPublishedBy(UserContext.getUserId());
        promptTemplateVersionDao.insert(versionEntity);
        promptService.evictTemplate(entity.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long id, Integer targetVersion) {
        PromptTemplateEntity entity = promptTemplateDao.findById(id);
        if (entity == null) {
            throw new BusinessException("模板不存在");
        }
        if (targetVersion == null || targetVersion <= 0) {
            throw new BusinessException("目标版本号无效");
        }
        PromptTemplateVersionEntity versionEntity = promptTemplateVersionDao.findByTemplateIdAndVersion(id, targetVersion);
        if (versionEntity == null) {
            throw new BusinessException("目标版本不存在");
        }
        entity.setContent(versionEntity.getContent());
        entity.setVariables(versionEntity.getVariables());
        entity.setStatus("PUBLISHED");
        entity.setVersion(targetVersion);
        promptTemplateDao.updateById(entity);
        promptService.evictTemplate(entity.getCode());
    }

    @Override
    public String test(Long id, PromptTestDTO dto) {
        PromptTemplateEntity entity = promptTemplateDao.findById(id);
        if (entity == null) {
            throw new BusinessException("模板不存在");
        }
        String rendered = promptService.renderTemplate(entity.getCode(), dto.getVariables());
        return llmClient.chat("你是 Prompt 测试助手。", rendered);
    }

    private PromptTemplateVO toVO(PromptTemplateEntity entity) {
        PromptTemplateVO vo = new PromptTemplateVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setCategory(entity.getCategory());
        vo.setStatus(entity.getStatus());
        vo.setVersion(entity.getVersion());
        vo.setContent(entity.getContent());
        vo.setVariables(entity.getVariables());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private PromptTemplateVersionVO toVersionVO(PromptTemplateVersionEntity entity) {
        PromptTemplateVersionVO vo = new PromptTemplateVersionVO();
        vo.setId(entity.getId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setVersion(entity.getVersion());
        vo.setContent(entity.getContent());
        vo.setVariables(entity.getVariables());
        vo.setPublishedBy(entity.getPublishedBy());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
