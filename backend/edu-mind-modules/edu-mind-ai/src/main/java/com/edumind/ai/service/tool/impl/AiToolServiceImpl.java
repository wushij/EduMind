package com.edumind.ai.service.tool.impl;

import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiToolDao;
import com.edumind.ai.entity.AiToolEntity;
import com.edumind.ai.gateway.ModelRouter;
import com.edumind.ai.service.tool.AiToolService;
import com.edumind.ai.vo.AiToolVO;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiToolServiceImpl implements AiToolService {

    private final AiToolDao aiToolDao;
    private final AiConverter aiConverter;
    private final ModelRouter modelRouter;

    @Override
    public List<AiToolVO> listTools(String category, String keyword) {
        // 「绑定模型」展示的是工具实际运行的模型：工具本身不单独绑定模型，
        // 统一由 AI 网关按「场景路由 → 平台默认模型」解析。这里回填网关解析出的
        // 真实模型，替换 ai_tool.model_id 里与运行时不符的历史占位值（如 deepseek-chat）。
        String runtimeModel = modelRouter.resolveModelDisplayName(null);
        return filterByUserRole(aiToolDao.list(category, keyword)).stream()
                .map(entity -> {
                    AiToolVO vo = aiConverter.toToolVO(entity);
                    vo.setModelId(runtimeModel);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<AiToolEntity> filterByUserRole(List<AiToolEntity> entities) {
        LoginUser user = UserContext.get();
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return entities;
        }
        if (user.getRoles().contains(SecurityConstant.ROLE_ADMIN)) {
            return entities;
        }

        boolean isTeacher = user.getRoles().contains(SecurityConstant.ROLE_TEACHER);
        boolean isStudent = user.getRoles().contains(SecurityConstant.ROLE_STUDENT);

        return entities.stream()
                .filter(entity -> isToolVisibleForUser(entity.getCategory(), isTeacher, isStudent))
                .collect(Collectors.toList());
    }

    private boolean isToolVisibleForUser(String category, boolean isTeacher, boolean isStudent) {
        if (!StringUtils.hasText(category) || "GENERAL".equalsIgnoreCase(category)) {
            return true;
        }
        if ("TEACHER".equalsIgnoreCase(category)) {
            return isTeacher;
        }
        if ("STUDENT".equalsIgnoreCase(category)) {
            return isStudent;
        }
        return true;
    }

    @Override
    public void recordToolUse(String id) {
        AiToolEntity entity = aiToolDao.findById(id);
        if (entity == null || entity.getStatus() == null || entity.getStatus() != 1) {
            throw new BusinessException("AI 工具不存在或已下架");
        }
        aiToolDao.incrementUseCount(id);
    }
}
