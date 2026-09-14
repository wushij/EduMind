package com.edumind.infrastructure.sms;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TencentSmsSender implements SmsSender {

    @Override
    public String getProviderName() {
        return "tencent";
    }

    @Override
    public boolean isConfigured(SmsConfig config) {
        return StringUtils.hasText(config.getAccessKeyId()) && StringUtils.hasText(config.getAccessKeySecret());
    }

    @Override
    public SmsSendResult sendVerifyCode(SmsConfig config, String phone, String code, String templateCode) {
        if (!isConfigured(config)) {
            log.warn("腾讯云短信配置不完整，使用控制台打印模式");
            log.info("【短信验证码 - 腾讯云(未配置)】phone={}, template={}, code={}", phone, templateCode, code);
            return SmsSendResult.ok("console", "控制台打印模式（密钥未配置）", null);
        }

        String bizId = null;
        String resultMsg = null;

        try {
            Credential cred = new Credential(config.getAccessKeyId().trim(), config.getAccessKeySecret().trim());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            httpProfile.setReqMethod("POST");
            httpProfile.setConnTimeout(60);

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            clientProfile.setSignMethod("HmacSHA256");

            SmsClient client = new SmsClient(cred, "ap-guangzhou", clientProfile);
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(config.getTencentAppId());
            request.setSignName(config.getSignName());
            request.setTemplateId(templateCode);
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            request.setTemplateParamSet(new String[]{code});

            SendSmsResponse response = client.SendSms(request);
            SendStatus[] sendStatusSet = response.getSendStatusSet();
            if (sendStatusSet != null && sendStatusSet.length > 0) {
                SendStatus status = sendStatusSet[0];
                bizId = status.getSerialNo();
                resultMsg = status.getMessage();
                if ("Ok".equals(status.getCode())) {
                    log.info("腾讯云短信发送成功: phone={}, template={}, serialNo={}", phone, templateCode, bizId);
                    return SmsSendResult.ok(getProviderName(), resultMsg, bizId);
                }
                log.error("腾讯云短信发送失败: template={}, code={}, message={}", templateCode, status.getCode(), resultMsg);
            } else {
                resultMsg = "响应结果为空";
                log.error("腾讯云短信发送失败: {}", resultMsg);
            }
        } catch (Exception e) {
            resultMsg = e.getMessage();
            log.error("腾讯云短信发送异常: template={}", templateCode, e);
        }
        return SmsSendResult.fail(getProviderName(), resultMsg != null ? resultMsg : "发送失败");
    }
}
