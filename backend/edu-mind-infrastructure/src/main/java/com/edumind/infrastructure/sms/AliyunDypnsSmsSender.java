package com.edumind.infrastructure.sms;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunDypnsSmsSender implements SmsSender {

    private final RedisService redisService;

    @Override
    public String getProviderName() {
        return "aliyunAuth";
    }

    @Override
    public boolean isConfigured(SmsConfig config) {
        return StringUtils.hasText(config.getAccessKeyId()) && StringUtils.hasText(config.getAccessKeySecret());
    }

    @Override
    public SmsSendResult sendVerifyCode(SmsConfig config, String phone, String code, String templateCode) {
        if (!isConfigured(config)) {
            log.warn("阿里云短信认证配置不完整，使用控制台打印模式");
            log.info("【短信验证码 - 阿里云短信认证(未配置)】phone={}, template={}, code={}", phone, templateCode, code);
            return SmsSendResult.ok("console", "控制台打印模式（密钥未配置）", null);
        }

        String signName = config.getSignName();
        int expireMinutes = config.getCodeExpireMinutes() != null ? config.getCodeExpireMinutes() : 5;
        String bizId = null;
        String resultMsg = null;

        try {
            Client client = createClient(config);
            String templateParam = String.format("{\"code\":\"%s\",\"min\":\"%d\"}", code, expireMinutes);
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam(templateParam)
                    .setPhoneNumber(phone);

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            SendSmsVerifyCodeResponseBody body = response.getBody();
            if (body != null && body.getModel() != null) {
                bizId = body.getModel().getBizId();
                cacheOutId(phone, body.getModel().getOutId(), expireMinutes);
            }
            if (body != null && (Boolean.TRUE.equals(body.getSuccess()) || "OK".equalsIgnoreCase(body.getCode()))) {
                log.info("阿里云短信认证发送成功: phone={}, template={}, bizId={}", phone, templateCode, bizId);
                return SmsSendResult.ok(getProviderName(), body.getMessage(), bizId);
            }
            resultMsg = body != null ? body.getMessage() : "响应为空";
            log.error("阿里云短信认证发送失败: template={}, code={}, message={}",
                    templateCode, body != null ? body.getCode() : null, resultMsg);
        } catch (TeaException e) {
            resultMsg = e.getMessage();
            log.error("阿里云短信认证发送异常: template={}, {}", templateCode, resultMsg, e);
        } catch (Exception e) {
            resultMsg = e.getMessage();
            log.error("阿里云短信认证发送异常: template={}", templateCode, e);
        }
        return SmsSendResult.fail(getProviderName(), resultMsg != null ? resultMsg : "发送失败");
    }

    private Client createClient(SmsConfig config) throws Exception {
        Config clientConfig = new Config()
                .setAccessKeyId(config.getAccessKeyId().trim())
                .setAccessKeySecret(config.getAccessKeySecret().trim())
                .setEndpoint("dypnsapi.aliyuncs.com");
        return new Client(clientConfig);
    }

    private void cacheOutId(String phone, String outId, int expireMinutes) {
        if (!StringUtils.hasText(outId)) {
            return;
        }
        redisService.set(RedisKeyBuilder.smsOutId(phone), outId, (long) expireMinutes * 60);
    }
}
