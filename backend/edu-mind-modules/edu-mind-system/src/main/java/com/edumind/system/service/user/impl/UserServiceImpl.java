package com.edumind.system.service.user.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.converter.UserConverter;
import com.edumind.system.dao.UserDao;
import com.edumind.system.dao.UserRoleDao;
import com.edumind.system.service.user.UserVoAssembler;
import com.edumind.system.dto.user.PasswordChangeDTO;
import com.edumind.system.dto.user.UserCreateDTO;
import com.edumind.system.dto.user.UserProfileUpdateDTO;
import com.edumind.system.dto.user.UserQueryDTO;
import com.edumind.system.dto.user.UserStatusUpdateDTO;
import com.edumind.system.dto.user.UserUpdateDTO;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.service.user.UserService;
import com.edumind.system.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final UserRoleDao userRoleDao;
    private final UserConverter userConverter;
    private final UserVoAssembler userVoAssembler;

    @Override
    public UserVO getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity entity = userDao.findById(userId);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        return userVoAssembler.toVO(entity);
    }

    @Override
    public UserVO updateProfile(UserProfileUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity entity = userDao.findById(userId);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(dto.getNickname())) {
            entity.setRealName(dto.getNickname());
        } else if (StringUtils.hasText(dto.getRealName())) {
            entity.setRealName(dto.getRealName());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            entity.setPhone(dto.getPhone());
        }
        if (dto.getAvatar() != null) {
            entity.setAvatar(dto.getAvatar());
        }
        userDao.updateById(entity);
        return userVoAssembler.toVO(entity);
    }

    @Override
    public void changePassword(PasswordChangeDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity entity = userDao.findById(userId);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        if (!matchesPassword(dto.getOldPassword(), entity.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        entity.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userDao.updateById(entity);
    }

    @Override
    public PageResult<UserVO> pageUsers(UserQueryDTO query) {
        Page<UserEntity> page = userDao.pageQuery(query);
        List<UserVO> list = page.getRecords().stream()
                .map(userVoAssembler::toVO)
                .collect(Collectors.toList());
        if (StringUtils.hasText(query.getRole())) {
            list = list.stream()
                    .filter(user -> user.getRoles() != null && user.getRoles().contains(query.getRole()))
                    .collect(Collectors.toList());
        }
        return PageResult.<UserVO>builder()
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .list(list)
                .build();
    }

    @Override
    public Long createUser(UserCreateDTO dto) {
        if (userDao.findByUsername(dto.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        UserEntity entity = userConverter.toEntity(dto);
        entity.setPassword(BCrypt.hashpw(dto.getPassword()));
        userDao.insert(entity);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            userRoleDao.replaceUserRoles(entity.getId(), dto.getRoleIds());
        }
        return entity.getId();
    }

    @Override
    public UserVO updateUser(Long id, UserUpdateDTO dto) {
        UserEntity entity = userDao.findById(id);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        userConverter.applyUpdate(entity, dto);
        userDao.updateById(entity);
        if (dto.getRoleIds() != null) {
            userRoleDao.replaceUserRoles(id, dto.getRoleIds());
        }
        return userVoAssembler.toVO(entity);
    }

    @Override
    public UserVO updateUserStatus(Long id, UserStatusUpdateDTO dto) {
        UserEntity entity = userDao.findById(id);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        entity.setStatus(dto.getStatus());
        userDao.updateById(entity);
        return userVoAssembler.toVO(entity);
    }

    @Override
    public UserVO getUserById(Long id) {
        UserEntity entity = userDao.findById(id);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        return userVoAssembler.toVO(entity);
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(storedPassword)) {
            return false;
        }
        try {
            if (BCrypt.checkpw(rawPassword, storedPassword)) {
                return true;
            }
        } catch (Exception ignored) {
            // 兼容非 BCrypt 存储
        }
        return rawPassword.equals(storedPassword);
    }
}
