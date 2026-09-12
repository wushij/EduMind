package com.edumind.system.service.user;

import com.edumind.common.api.PageResult;
import com.edumind.system.dto.user.PasswordChangeDTO;
import com.edumind.system.dto.user.UserCreateDTO;
import com.edumind.system.dto.user.UserProfileUpdateDTO;
import com.edumind.system.dto.user.UserQueryDTO;
import com.edumind.system.dto.user.UserStatusUpdateDTO;
import com.edumind.system.dto.user.UserUpdateDTO;
import com.edumind.system.vo.user.UserVO;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserVO getProfile();

    UserVO updateProfile(UserProfileUpdateDTO dto);

    UserVO uploadAvatar(MultipartFile file);

    void sendBindEmailCode(String email);

    UserVO bindEmail(com.edumind.system.dto.user.EmailBindDTO dto);

    void changePassword(PasswordChangeDTO dto);

    PageResult<UserVO> pageUsers(UserQueryDTO query);

    Long createUser(UserCreateDTO dto);

    UserVO updateUser(Long id, UserUpdateDTO dto);

    UserVO updateUserStatus(Long id, UserStatusUpdateDTO dto);

    UserVO getUserById(Long id);

    void deleteUser(Long id);
}
