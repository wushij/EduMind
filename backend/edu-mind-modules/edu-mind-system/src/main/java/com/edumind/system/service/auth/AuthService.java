package com.edumind.system.service.auth;

import com.edumind.system.dto.auth.LoginDTO;
import com.edumind.system.dto.auth.RegisterDTO;
import com.edumind.system.vo.auth.LoginVO;
import com.edumind.system.vo.user.UserVO;

/**
 * 认证授权业务服务接口
 */
public interface AuthService {

    LoginVO login(LoginDTO loginDTO);

    Long register(RegisterDTO registerDTO);

    void logout();

    UserVO getUserInfo();
}
