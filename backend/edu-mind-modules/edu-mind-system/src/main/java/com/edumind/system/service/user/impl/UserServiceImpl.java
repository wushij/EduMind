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
import com.edumind.system.entity.UserEntity;
import com.edumind.system.service.user.UserService;
import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.system.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final UserRoleDao userRoleDao;
    private final UserConverter userConverter;
    private final UserVoAssembler userVoAssembler;
    private final com.edumind.system.service.email.EmailCodeService emailCodeService;
    private final FileStorageService fileStorageService;

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
    public UserVO uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传的头像文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只支持图片格式文件 (JPG/PNG/GIF/WebP等)");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("头像文件大小不能超过 5MB");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity entity = userDao.findById(userId);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "png";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        String username = StringUtils.hasText(entity.getUsername())
                ? entity.getUsername().trim().toLowerCase().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "user_" + userId;
        String objectKey = String.format("users/%s/avatar/avatar_%s.%s",
                username,
                UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                ext);

        try {
            String avatarUrl = fileStorageService.uploadFile("edumind", objectKey, file.getInputStream(), contentType);
            entity.setAvatar(avatarUrl);
            userDao.updateById(entity);
            return userVoAssembler.toVO(entity);
        } catch (IOException e) {
            throw new BusinessException("头像上传处理失败: " + e.getMessage());
        }
    }

    @Override
    public void sendBindEmailCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱地址不能为空");
        }
        String cleanEmail = email.trim().toLowerCase();
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 检查该邮箱是否已被其他人占用绑定
        UserEntity existing = userDao.findByEmail(cleanEmail);
        if (existing != null && !existing.getId().equals(currentUserId)) {
            throw new BusinessException("该邮箱已被其他账号绑定，请更换其他邮箱");
        }

        emailCodeService.sendCode(com.edumind.system.dto.auth.EmailSendCodeDTO.builder()
                .email(cleanEmail)
                .scene("bind")
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO bindEmail(com.edumind.system.dto.user.EmailBindDTO dto) {
        String cleanEmail = dto.getEmail().trim().toLowerCase();
        String code = dto.getCode().trim();
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 1. 核验绑定专属验证码
        emailCodeService.verifyCode(cleanEmail, "bind", code);

        // 2. 检查唯一性
        UserEntity existing = userDao.findByEmail(cleanEmail);
        if (existing != null && !existing.getId().equals(currentUserId)) {
            throw new BusinessException("该邮箱已被其他账号绑定，请更换其他邮箱");
        }

        // 3. 更新当前用户邮箱
        UserEntity currentUser = userDao.findById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }
        currentUser.setEmail(cleanEmail);
        userDao.updateById(currentUser);

        return userVoAssembler.toVO(currentUser);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (id.equals(currentUserId)) {
            throw new BusinessException("不能删除当前登录账号");
        }
        UserEntity entity = userDao.findById(id);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        if ("admin".equalsIgnoreCase(entity.getUsername())) {
            throw new BusinessException("系统内置管理员账号不允许删除");
        }
        userRoleDao.deleteByUserId(id);
        userDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        UserEntity entity = userDao.findById(id);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        entity.setPassword(BCrypt.hashpw(newPassword));
        userDao.updateById(entity);
        try {
            StpUtil.kickout(id);
        } catch (Exception ignored) {
        }
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
