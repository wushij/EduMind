package com.edumind.system.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.system.dto.user.UserQueryDTO;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 用户 DAO 统一访问层
 */
@Repository
@RequiredArgsConstructor
public class UserDao {

    private final UserMapper userMapper;

    public UserEntity findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
    }

    public UserEntity findById(Long id) {
        return userMapper.selectById(id);
    }

    public int insert(UserEntity entity) {
        return userMapper.insert(entity);
    }

    public int updateById(UserEntity entity) {
        return userMapper.updateById(entity);
    }

    public Page<UserEntity> pageQuery(UserQueryDTO query) {
        long pageNum = query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1L;
        long pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10L;
        Page<UserEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(UserEntity::getUsername, query.getKeyword())
                    .or()
                    .like(UserEntity::getRealName, query.getKeyword())
                    .or()
                    .like(UserEntity::getEmail, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(UserEntity::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(UserEntity::getCreateTime);
        return userMapper.selectPage(page, wrapper);
    }
}
