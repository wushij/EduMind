package com.edumind.system.service.sms;

import com.edumind.infrastructure.sms.ConsoleSmsSender;
import com.edumind.infrastructure.sms.SmsConfig;
import com.edumind.infrastructure.sms.SmsSendResult;
import com.edumind.infrastructure.sms.SmsSender;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSenderFactory {

    private final List<SmsSender> smsSenders;
    private final SmsLogService smsLogService;

    private final Map<String, SmsSender> senderMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (SmsSender sender : smsSenders) {
            senderMap.put(sender.getProviderName(), sender);
            log.info("注册短信服务: {}", sender.getProviderName());
        }
        SmsSender aliyunAuth = senderMap.get("aliyunAuth");
        if (aliyunAuth != null) {
            senderMap.put("aliyun", aliyunAuth);
        }
    }

    public boolean sendCode(SmsConfig config, String phone, String code, String templateCode) {
        SmsSender sender = resolveSender(config);
        String template = StringUtils.hasText(templateCode) ? templateCode.trim() : defaultTemplate(config);
        SmsSendResult result = sender.sendVerifyCode(config, phone, code, template);
        smsLogService.logVerifyCode(
                phone,
                code,
                template,
                result.getProvider(),
                result.isSuccess(),
                result.getMessage(),
                result.getBizId(),
                "TEST_VERIFY"
        );
        return result.isSuccess();
    }

    private SmsSender resolveSender(SmsConfig config) {
        String provider = config.getProvider() != null ? config.getProvider().trim() : "aliyunAuth";
        if ("aliyun".equals(provider)) {
            provider = "aliyunAuth";
        }
        SmsSender sender = senderMap.get(provider);
        if (sender == null) {
            log.warn("未找到短信服务商: {}，使用控制台模式", provider);
            sender = senderMap.get("console");
        }
        return sender != null ? sender : senderMap.get("console");
    }

    private String defaultTemplate(SmsConfig config) {
        if (StringUtils.hasText(config.getTemplateVerifyCode())) {
            return config.getTemplateVerifyCode().trim();
        }
        return "100001";
    }
}
