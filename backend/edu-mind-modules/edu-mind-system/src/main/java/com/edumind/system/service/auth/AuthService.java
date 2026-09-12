package com.edumind.system.service.auth;

import com.edumind.system.dto.auth.EmailLoginDTO;
import com.edumind.system.dto.auth.EmailSendCodeDTO;
import com.edumind.system.dto.auth.LoginDTO;
import com.edumind.system.dto.auth.RegisterDTO;
import com.edumind.system.vo.auth.LoginVO;
import com.edumind.system.vo.user.UserVO;

/**
 * 认证授权业务服务接口
 */
public interface AuthService {

    LoginVO login(LoginDTO loginDTO);

    LoginVO emailLogin(EmailLoginDTO emailLoginDTO);

    void sendEmailCode(EmailSendCodeDTO sendCodeDTO);

    Long register(RegisterDTO registerDTO);

    void logout();

    UserVO getUserInfo();
}
