package com.edumind.system.service.sms.impl;

import com.edumind.common.utils.IpUtils;
import com.edumind.system.dao.SysSmsLogDao;
import com.edumind.system.entity.SysSmsLogEntity;
import com.edumind.system.service.sms.SmsLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SmsLogServiceImpl implements SmsLogService {

    private final SysSmsLogDao sysSmsLogDao;

    @Override
    public void logVerifyCode(String phone, String code, String templateId, String provider,
                              boolean success, String resultMsg, String bizId, String smsType) {
        String ip = resolveClientIp();
        SysSmsLogEntity entity = SysSmsLogEntity.builder()
                .phone(phone)
                .content(code)
                .smsType(smsType != null ? smsType : "verify_code")
                .templateId(templateId)
                .provider(provider)
                .status(success ? 1 : 2)
                .resultMsg(resultMsg)
                .bizId(bizId)
                .sendTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .bizType("config_test")
                .ip(ip)
                .build();
        sysSmsLogDao.insert(entity);
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request != null ? IpUtils.getClientIp(request) : null;
    }
}
