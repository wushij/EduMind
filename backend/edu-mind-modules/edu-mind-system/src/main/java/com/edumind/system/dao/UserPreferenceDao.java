package com.edumind.system.dao;

import com.edumind.system.entity.UserPreferenceEntity;
import com.edumind.system.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserPreferenceDao {

    private final UserPreferenceMapper userPreferenceMapper;

    public UserPreferenceEntity findByUserId(Long userId) {
        return userPreferenceMapper.selectById(userId);
    }

    /** 批量按用户 ID 查询偏好（单次 IN 查询），用于替代循环内逐条查询 */
    public List<UserPreferenceEntity> findByUserIds(java.util.Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return userPreferenceMapper.selectBatchIds(userIds);
    }

    public int insert(UserPreferenceEntity entity) {
        return userPreferenceMapper.insert(entity);
    }

    public int updateById(UserPreferenceEntity entity) {
        return userPreferenceMapper.updateById(entity);
    }
}
