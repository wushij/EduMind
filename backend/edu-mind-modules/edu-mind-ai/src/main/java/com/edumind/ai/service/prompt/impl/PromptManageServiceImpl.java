package com.edumind.ai.service.prompt.impl;

import com.edumind.ai.dao.PromptTemplateDao;
import com.edumind.ai.dao.PromptTemplateVersionDao;
import com.edumind.ai.dto.prompt.PromptTemplateDTO;
import com.edumind.ai.dto.prompt.PromptTestDTO;
import com.edumind.ai.entity.PromptTemplateEntity;
import com.edumind.ai.entity.PromptTemplateVersionEntity;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.ai.integration.llm.MockLlmClient;
import com.edumind.ai.gateway.ModelRouter;
import com.edumind.ai.prompt.AiPromptConstants;
import com.edumind.ai.service.prompt.PromptManageService;
import com.edumind.ai.service.question.ExamRagOutputNormalizer;
import com.edumind.ai.service.teaching.LessonPrepOutputNormalizer;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.vo.prompt.PromptTemplateVO;
import com.edumind.ai.vo.prompt.PromptTemplateVersionVO;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromptManageServiceImpl implements PromptManageService {

    private final PromptTemplateDao promptTemplateDao;
    private final PromptTemplateVersionDao promptTemplateVersionDao;
    private final PromptService promptService;
    private final ModelRouter modelRouter;
    private final LlmClientRegistry llmClientRegistry;
    private final ExamRagOutputNormalizer examRagOutputNormalizer;
    private final LessonPrepOutputNormalizer lessonPrepOutputNormalizer;

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
        entity.setDescription(dto.getDescription());
        entity.setSystemPrompt(dto.getSystemPrompt());
        entity.setContent(dto.getContent());
        entity.setVariables(dto.getVariables());
        entity.setBoundModel(StringUtils.hasText(dto.getBoundModel()) ? dto.getBoundModel() : "deepseek-chat");
        entity.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : new java.math.BigDecimal("0.30"));
        entity.setMaxTokens(dto.getMaxTokens() != null ? dto.getMaxTokens() : 8000);
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
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getSystemPrompt() != null) {
            entity.setSystemPrompt(dto.getSystemPrompt());
        }
        if (dto.getContent() != null) {
            entity.setContent(dto.getContent());
        }
        if (dto.getVariables() != null) {
            entity.setVariables(dto.getVariables());
        }
        if (dto.getBoundModel() != null) {
            entity.setBoundModel(dto.getBoundModel());
        }
        if (dto.getTemperature() != null) {
            entity.setTemperature(dto.getTemperature());
        }
        if (dto.getMaxTokens() != null) {
            entity.setMaxTokens(dto.getMaxTokens());
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

        List<PromptTemplateVersionEntity> history = promptTemplateVersionDao.listByTemplateId(id);
        int nextVersion;
        if (history == null || history.isEmpty()) {
            // 首次归档快照：若实体已有初始版本号则继承（默认1），避免跳变到v2
            nextVersion = (entity.getVersion() != null && entity.getVersion() > 0) ? entity.getVersion() : 1;
        } else {
            PromptTemplateVersionEntity latest = history.get(0);
            boolean sysSame = Objects.equals(StringUtils.trimWhitespace(entity.getSystemPrompt()), StringUtils.trimWhitespace(latest.getSystemPrompt()));
            boolean contentSame = Objects.equals(StringUtils.trimWhitespace(entity.getContent()), StringUtils.trimWhitespace(latest.getContent()));
            boolean varsSame = Objects.equals(StringUtils.trimWhitespace(entity.getVariables()), StringUtils.trimWhitespace(latest.getVariables()));

            if (sysSame && contentSame && varsSame && "PUBLISHED".equalsIgnoreCase(entity.getStatus())) {
                // 内容与当前最新发布快照完全一致，直接更新缓存，杜绝生成冗余无意义重复版本
                promptService.evictTemplate(entity.getCode());
                return;
            }
            nextVersion = (latest.getVersion() != null ? latest.getVersion() : 1) + 1;
        }

        entity.setStatus("PUBLISHED");
        entity.setVersion(nextVersion);
        promptTemplateDao.updateById(entity);

        PromptTemplateVersionEntity versionEntity = new PromptTemplateVersionEntity();
        versionEntity.setTemplateId(entity.getId());
        versionEntity.setVersion(nextVersion);
        versionEntity.setContent(entity.getContent());
        versionEntity.setSystemPrompt(entity.getSystemPrompt());
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
        if (versionEntity.getSystemPrompt() != null) {
            entity.setSystemPrompt(versionEntity.getSystemPrompt());
        }
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

        // 1. 优先使用前端传入的实时 User 指令，否则渲染模板 content 字段
        String rendered;
        if (dto != null && StringUtils.hasText(dto.getUserPromptTemplate())) {
            rendered = dto.getUserPromptTemplate();
            if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
                for (java.util.Map.Entry<String, String> entry : dto.getVariables().entrySet()) {
                    rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
                }
            }
        } else {
            rendered = promptService.renderUserContent(entity.getCode(), dto != null ? dto.getVariables() : null);
            if (!StringUtils.hasText(rendered)) {
                rendered = promptService.renderTemplate(entity.getCode(), dto != null ? dto.getVariables() : null);
            }
        }

        // 2. 优先使用前端传入的实时 System Prompt，否则渲染模板 system_prompt
        String systemPrompt = (dto != null && StringUtils.hasText(dto.getSystemPrompt()))
                ? dto.getSystemPrompt()
                : promptService.renderSystemPrompt(entity.getCode(), dto != null ? dto.getVariables() : null);
        if (!StringUtils.hasText(systemPrompt)) {
            systemPrompt = "你是 EduMind AI 教学提示词测试助手。";
        }
        if (dto != null && dto.getVariables() != null && !dto.getVariables().isEmpty() && StringUtils.hasText(systemPrompt)) {
            for (java.util.Map.Entry<String, String> entry : dto.getVariables().entrySet()) {
                systemPrompt = systemPrompt.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
            }
        }

        // 3. 动态模型路由：优先前端选中 modelKey，否则模板 boundModel，再否则系统默认 chat 模型
        String targetModelKey = (dto != null && StringUtils.hasText(dto.getModelKey()))
                ? dto.getModelKey()
                : entity.getBoundModel();
        String resolvedModelKey = modelRouter.resolveModelKey("CHAT", targetModelKey);
        LlmClient targetClient = llmClientRegistry.get(resolvedModelKey);
        if (targetClient instanceof MockLlmClient && !"mock".equalsIgnoreCase(resolvedModelKey)) {
            throw new BusinessException("模型「" + resolvedModelKey + "」未配置 API Key 或不可用，请在【系统设置 → 模型管理】中配置后重试");
        }

        Double temperature = dto != null && dto.getTemperature() != null
                ? dto.getTemperature()
                : (entity.getTemperature() != null ? entity.getTemperature().doubleValue() : null);
        Integer maxTokens = dto != null && dto.getMaxTokens() != null && dto.getMaxTokens() > 0
                ? dto.getMaxTokens()
                : entity.getMaxTokens();
        // Prompt 沙箱走同步 chat()，需关闭 thinking，否则 Flash 等模型可能把 token 预算耗在 reasoning 上导致 content 为空
        com.edumind.ai.integration.llm.LlmChatOptions options = com.edumind.ai.integration.llm.LlmChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .disableThinking(true)
                .build();

        String output = targetClient.chat(systemPrompt, rendered, options);
        if (AiPromptConstants.EXAM_RAG_GENERAL.equals(entity.getCode())) {
            output = examRagOutputNormalizer.normalize(output, dto != null ? dto.getVariables() : null);
        }
        if (AiPromptConstants.LESSON_PREP_RAG_GENERAL.equals(entity.getCode())) {
            output = lessonPrepOutputNormalizer.normalize(output, dto != null ? dto.getVariables() : null);
        }
        return output;
    }

    private PromptTemplateVO toVO(PromptTemplateEntity entity) {
        PromptTemplateVO vo = new PromptTemplateVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setCategory(entity.getCategory());
        vo.setDescription(entity.getDescription());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setStatus(entity.getStatus());
        vo.setVersion(entity.getVersion());
        vo.setContent(entity.getContent());
        vo.setVariables(entity.getVariables());
        vo.setBoundModel(entity.getBoundModel());
        vo.setTemperature(entity.getTemperature());
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private PromptTemplateVersionVO toVersionVO(PromptTemplateVersionEntity entity) {
        PromptTemplateVersionVO vo = new PromptTemplateVersionVO();
        vo.setId(entity.getId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setVersion(entity.getVersion());
        vo.setContent(entity.getContent());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setVariables(entity.getVariables());
        vo.setPublishedBy(entity.getPublishedBy());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
