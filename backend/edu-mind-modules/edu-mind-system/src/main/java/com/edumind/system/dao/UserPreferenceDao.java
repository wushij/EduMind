package com.edumind.system.dao;

import com.edumind.system.entity.UserPreferenceEntity;
import com.edumind.system.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPreferenceDao {

    private final UserPreferenceMapper userPreferenceMapper;

    public UserPreferenceEntity findByUserId(Long userId) {
        return userPreferenceMapper.selectById(userId);
    }

    public int insert(UserPreferenceEntity entity) {
        return userPreferenceMapper.insert(entity);
    }

    public int updateById(UserPreferenceEntity entity) {
        return userPreferenceMapper.updateById(entity);
    }
}
