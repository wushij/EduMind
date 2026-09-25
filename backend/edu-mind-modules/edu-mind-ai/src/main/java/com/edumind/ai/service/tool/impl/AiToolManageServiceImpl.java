package com.edumind.ai.service.tool.impl;

import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.dao.AiToolDao;
import com.edumind.ai.dto.tool.AiToolFlagsDTO;
import com.edumind.ai.dto.tool.AiToolSaveDTO;
import com.edumind.ai.dto.tool.AiToolUpdateDTO;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.entity.AiToolEntity;
import com.edumind.ai.service.tool.AiToolManageService;
import com.edumind.ai.vo.tool.AiToolAdminVO;
import com.edumind.ai.vo.tool.AiToolStatsVO;
import com.edumind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiToolManageServiceImpl implements AiToolManageService {

    private static final Set<String> SEED_TOOL_IDS = Set.of(
            "tool_question_gen", "tool_exam_gen", "tool_grading", "tool_summary",
            "tool_chat", "tool_practice"
    );

    private static final Set<String> ALLOWED_CATEGORIES = Set.of("TEACHER", "STUDENT", "GENERAL");
    private static final Set<String> ALLOWED_EXECUTION_MODES = Set.of("ROUTE", "V05_NOTICE");

    private final AiToolDao aiToolDao;
    private final AiModelConfigDao aiModelConfigDao;
    private final AiConverter aiConverter;

    @Override
    public List<AiToolAdminVO> list(String category, Integer status, String keyword, Boolean isRecommended) {
        return aiToolDao.listForAdmin(category, status, keyword, isRecommended).stream()
                .map(aiConverter::toAdminVO)
                .collect(Collectors.toList());
    }

    @Override
    public AiToolStatsVO stats() {
        List<AiToolEntity> all = aiToolDao.listAll();
        AiToolStatsVO stats = new AiToolStatsVO();
        stats.setTotal(all.size());
        stats.setOnline((int) all.stream().filter(e -> e.getStatus() != null && e.getStatus() == 1).count());
        stats.setOffline(stats.getTotal() - stats.getOnline());
        stats.setTeacherCount((int) all.stream().filter(e -> "TEACHER".equalsIgnoreCase(e.getCategory())).count());
        stats.setStudentCount((int) all.stream().filter(e -> "STUDENT".equalsIgnoreCase(e.getCategory())).count());
        stats.setGeneralCount((int) all.stream().filter(e -> "GENERAL".equalsIgnoreCase(e.getCategory())).count());
        stats.setTotalUseCount(all.stream()
                .mapToLong(e -> e.getUseCount() == null ? 0 : e.getUseCount())
                .sum());
        return stats;
    }

    @Override
    public AiToolAdminVO getById(String id) {
        AiToolEntity entity = requireTool(id);
        return aiConverter.toAdminVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(AiToolSaveDTO dto) {
        if (aiToolDao.existsById(dto.getId())) {
            throw new BusinessException("工具 ID 已存在");
        }
        validateCategory(dto.getCategory());
        validateExecutionMode(dto.getExecutionMode());
        validateModelId(dto.getModelId());
        normalizeRoute(dto);

        AiToolEntity entity = aiConverter.toEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        aiToolDao.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, AiToolUpdateDTO dto) {
        AiToolEntity entity = requireTool(id);
        validateCategory(dto.getCategory());
        validateExecutionMode(dto.getExecutionMode());
        validateModelId(dto.getModelId());
        normalizeRouteForUpdate(id, dto);

        aiConverter.applyUpdate(entity, dto);
        aiToolDao.update(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(String id) {
        AiToolEntity entity = requireTool(id);
        entity.setStatus(1);
        aiToolDao.update(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(String id) {
        AiToolEntity entity = requireTool(id);
        entity.setStatus(0);
        aiToolDao.update(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFlags(String id, AiToolFlagsDTO dto) {
        AiToolEntity entity = requireTool(id);
        if (dto.getIsRecommended() != null) {
            entity.setIsRecommended(Boolean.TRUE.equals(dto.getIsRecommended()) ? 1 : 0);
        }
        if (dto.getIsHot() != null) {
            entity.setIsHot(Boolean.TRUE.equals(dto.getIsHot()) ? 1 : 0);
        }
        aiToolDao.update(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        if (SEED_TOOL_IDS.contains(id)) {
            throw new BusinessException("内置种子工具不可删除，请使用下架操作");
        }
        if (!aiToolDao.existsById(id)) {
            throw new BusinessException("AI 工具不存在");
        }
        aiToolDao.deleteById(id);
    }

    private AiToolEntity requireTool(String id) {
        AiToolEntity entity = aiToolDao.findById(id);
        if (entity == null) {
            throw new BusinessException("AI 工具不存在");
        }
        return entity;
    }

    private void validateCategory(String category) {
        if (!StringUtils.hasText(category) || !ALLOWED_CATEGORIES.contains(category.toUpperCase())) {
            throw new BusinessException("工具分类必须为 TEACHER、STUDENT 或 GENERAL");
        }
    }

    private void validateExecutionMode(String executionMode) {
        String mode = StringUtils.hasText(executionMode) ? executionMode : "ROUTE";
        if (!ALLOWED_EXECUTION_MODES.contains(mode)) {
            throw new BusinessException("执行模式必须为 ROUTE 或 V05_NOTICE");
        }
    }

    private void validateModelId(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return;
        }
        AiModelConfigEntity model = aiModelConfigDao.findByModelKey(modelId);
        if (model == null) {
            model = aiModelConfigDao.findByConfigName(modelId);
        }
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            throw new BusinessException("绑定的 AI 模型不存在或未启用");
        }
    }

    private void normalizeRoute(AiToolSaveDTO dto) {
        String mode = StringUtils.hasText(dto.getExecutionMode()) ? dto.getExecutionMode() : "ROUTE";
        if ("V05_NOTICE".equals(mode) && !StringUtils.hasText(dto.getRoute())) {
            dto.setRoute("/ai/marketplace/v05/" + dto.getId());
            return;
        }
        if ("ROUTE".equals(mode) && !StringUtils.hasText(dto.getRoute())) {
            throw new BusinessException("ROUTE 模式下必须填写前端路由");
        }
    }

    private void normalizeRouteForUpdate(String id, AiToolUpdateDTO dto) {
        String mode = StringUtils.hasText(dto.getExecutionMode()) ? dto.getExecutionMode() : "ROUTE";
        if ("V05_NOTICE".equals(mode) && !StringUtils.hasText(dto.getRoute())) {
            dto.setRoute("/ai/marketplace/v05/" + id);
            return;
        }
        if ("ROUTE".equals(mode) && !StringUtils.hasText(dto.getRoute())) {
            throw new BusinessException("ROUTE 模式下必须填写前端路由");
        }
    }
}
