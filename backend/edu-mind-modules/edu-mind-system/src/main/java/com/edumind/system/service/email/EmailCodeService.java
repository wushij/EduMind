package com.edumind.system.service.email;

import com.edumind.system.dto.auth.EmailSendCodeDTO;

/**
 * 邮箱验证码服务接口：生成、频控、校验与核销
 */
public interface EmailCodeService {

    /**
     * 发送邮箱验证码（内置频控、单日限额与异步邮件投递）
     */
    void sendCode(EmailSendCodeDTO dto);

    /**
     * 核验邮箱验证码（核验成功后单次销毁）
     */
    void verifyCode(String email, String scene, String code);
}
