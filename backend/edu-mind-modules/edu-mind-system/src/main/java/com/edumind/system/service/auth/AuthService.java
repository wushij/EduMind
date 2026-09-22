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

    /**
     * 演示扫码登录（登录页「模拟扫码授权通过(Demo)」专用）。
     * 免密，但账号由 sys.login.config 的 mockScanAccount 指定、并由 mockScanEnabled 控制开关，
     * 避免像旧实现那样在前端硬编码账号密码（密码一改按钮就失效）。
     */
    LoginVO demoScanLogin();

    LoginVO emailLogin(EmailLoginDTO emailLoginDTO);

    void sendEmailCode(EmailSendCodeDTO sendCodeDTO);

    Long register(RegisterDTO registerDTO);

    void logout();

    UserVO getUserInfo();

    com.edumind.system.vo.auth.PasswordResetVerifyVO verifyResetCode(com.edumind.system.dto.auth.PasswordResetVerifyDTO dto);

    void resetPassword(com.edumind.system.dto.auth.PasswordResetDTO dto);
}
