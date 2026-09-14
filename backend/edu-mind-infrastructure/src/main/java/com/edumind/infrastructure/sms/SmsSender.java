package com.edumind.infrastructure.sms;

/**
 * 短信发送底层适配（阿里云短信认证 / 腾讯云 / 控制台）
 */
public interface SmsSender {

    String getProviderName();

    boolean isConfigured(SmsConfig config);

    SmsSendResult sendVerifyCode(SmsConfig config, String phone, String code, String templateCode);
}
