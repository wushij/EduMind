package com.edumind.system.converter;

import com.edumind.system.dto.user.UserCreateDTO;
import com.edumind.system.dto.user.UserUpdateDTO;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.vo.user.UserVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Component
public class UserConverter {

    public UserVO toVO(UserEntity entity, List<String> roles, List<String> permissions) {
        if (entity == null) {
            return null;
        }
        return UserVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .realName(StringUtils.hasText(entity.getRealName()) ? entity.getRealName() : entity.getUsername())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .status(entity.getStatus())
                .avatar(StringUtils.hasText(entity.getAvatar()) ? entity.getAvatar() : "")
                .roles(roles != null ? roles : Collections.emptyList())
                .permissions(permissions != null ? permissions : Collections.emptyList())
                .build();
    }

    public UserEntity toEntity(UserCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setRealName(dto.getRealName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setAvatar(dto.getAvatar());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLE");
        return entity;
    }

    public void applyUpdate(UserEntity entity, UserUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (StringUtils.hasText(dto.getRealName())) {
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
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
    }
}
