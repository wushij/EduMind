package com.edumind.infrastructure.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleSmsSender implements SmsSender {

    @Override
    public String getProviderName() {
        return "console";
    }

    @Override
    public boolean isConfigured(SmsConfig config) {
        return true;
    }

    @Override
    public SmsSendResult sendVerifyCode(SmsConfig config, String phone, String code, String templateCode) {
        log.info("============================================");
        log.info("【短信验证码 - 控制台模式】");
        log.info("手机号: {}", phone);
        if (templateCode != null && !templateCode.isBlank()) {
            log.info("模板: {}", templateCode);
        }
        log.info("验证码: {}", code);
        log.info("有效期: {} 分钟", config.getCodeExpireMinutes() != null ? config.getCodeExpireMinutes() : 5);
        log.info("============================================");
        return SmsSendResult.ok(getProviderName(), "控制台打印模式", null);
    }
}
