package com.edumind.system.service.sms;

/**
 * 短信发送审计日志
 */
public interface SmsLogService {

    void logVerifyCode(String phone, String code, String templateId, String provider,
                       boolean success, String resultMsg, String bizId, String smsType);
}
