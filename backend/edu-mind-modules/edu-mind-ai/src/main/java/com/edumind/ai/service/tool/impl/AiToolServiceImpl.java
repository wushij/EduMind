package com.edumind.ai.service.tool.impl;

import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiToolDao;
import com.edumind.ai.entity.AiToolEntity;
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

    @Override
    public List<AiToolVO> listTools(String category, String keyword) {
        return filterByUserRole(aiToolDao.list(category, keyword)).stream()
                .map(aiConverter::toToolVO)
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
