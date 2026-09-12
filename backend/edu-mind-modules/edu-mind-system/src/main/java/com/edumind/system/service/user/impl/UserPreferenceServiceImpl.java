package com.edumind.system.service.user.impl;

import com.edumind.common.model.UserContext;
import com.edumind.system.dao.UserPreferenceDao;
import com.edumind.system.dto.user.UserPreferenceDTO;
import com.edumind.system.entity.UserPreferenceEntity;
import com.edumind.system.service.user.UserPreferenceService;
import com.edumind.system.vo.user.UserPreferenceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceDao userPreferenceDao;

    @Override
    public UserPreferenceVO getMyPreferences() {
        Long userId = UserContext.getUserId();
        UserPreferenceEntity entity = userPreferenceDao.findByUserId(userId);
        if (entity == null) {
            return defaultPreference();
        }
        return toVO(entity);
    }

    @Override
    public UserPreferenceVO saveMyPreferences(UserPreferenceDTO dto) {
        Long userId = UserContext.getUserId();
        UserPreferenceEntity entity = userPreferenceDao.findByUserId(userId);
        boolean isNew = entity == null;
        if (isNew) {
            entity = new UserPreferenceEntity();
            entity.setUserId(userId);
        }
        if (dto.getTheme() != null) {
            entity.setTheme(dto.getTheme());
        }
        if (dto.getLanguage() != null) {
            entity.setLanguage(dto.getLanguage());
        }
        if (dto.getDefaultModel() != null) {
            entity.setDefaultModel(dto.getDefaultModel());
        }
        if (dto.getEnableRag() != null) {
            entity.setEnableRag(dto.getEnableRag() ? 1 : 0);
        }
        if (dto.getEnableNotification() != null) {
            entity.setEnableNotification(dto.getEnableNotification() ? 1 : 0);
        }
        if (dto.getPreferencesJson() != null) {
            entity.setPreferencesJson(dto.getPreferencesJson());
        }
        if (isNew) {
            userPreferenceDao.insert(entity);
        } else {
            userPreferenceDao.updateById(entity);
        }
        return toVO(entity);
    }

    private UserPreferenceVO defaultPreference() {
        UserPreferenceVO vo = new UserPreferenceVO();
        vo.setTheme("LIGHT");
        vo.setLanguage("zh-CN");
        vo.setDefaultModel("deepseek-v3");
        vo.setEnableRag(true);
        vo.setEnableNotification(true);
        return vo;
    }

    private UserPreferenceVO toVO(UserPreferenceEntity entity) {
        UserPreferenceVO vo = new UserPreferenceVO();
        vo.setTheme(entity.getTheme());
        vo.setLanguage(entity.getLanguage());
        vo.setDefaultModel(entity.getDefaultModel());
        vo.setEnableRag(entity.getEnableRag() == null || entity.getEnableRag() == 1);
        vo.setEnableNotification(entity.getEnableNotification() == null || entity.getEnableNotification() == 1);
        vo.setPreferencesJson(entity.getPreferencesJson());
        return vo;
    }
}
