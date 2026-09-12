package com.edumind.system.converter;

import com.edumind.infrastructure.mail.MailConfig;
import com.edumind.system.dto.config.MailConfigDTO;
import com.edumind.system.vo.config.MailConfigVO;
import org.springframework.stereotype.Component;

/**
 * 系统配置对象转换器
 */
@Component
public class SysConfigConverter {

    public MailConfigVO toMailVO(MailConfig config) {
        if (config == null) {
            return new MailConfigVO();
        }
        boolean hasPwd = config.getPassword() != null && !config.getPassword().isBlank();
        return MailConfigVO.builder()
                .enabled(config.getEnabled())
                .host(config.getHost())
                .port(config.getPort())
                .username(config.getUsername())
                .password(hasPwd ? "******" : "")
                .hasPassword(hasPwd)
                .fromName(config.getFromName())
                .useSsl(config.getUseSsl())
                .codeExpireMinutes(config.getCodeExpireMinutes())
                .codeIntervalSeconds(config.getCodeIntervalSeconds())
                .dailyLimitPerEmail(config.getDailyLimitPerEmail())
                .build();
    }

    public void applyMailUpdate(MailConfig target, MailConfigDTO dto) {
        if (target == null || dto == null) {
            return;
        }
        target.setEnabled(dto.getEnabled());
        target.setHost(dto.getHost());
        target.setPort(dto.getPort());
        target.setUsername(dto.getUsername());
        target.setFromName(dto.getFromName());
        target.setUseSsl(dto.getUseSsl());

        if (dto.getCodeExpireMinutes() != null && dto.getCodeExpireMinutes() > 0) {
            target.setCodeExpireMinutes(dto.getCodeExpireMinutes());
        }
        if (dto.getCodeIntervalSeconds() != null && dto.getCodeIntervalSeconds() > 0) {
            target.setCodeIntervalSeconds(dto.getCodeIntervalSeconds());
        }
        if (dto.getDailyLimitPerEmail() != null && dto.getDailyLimitPerEmail() > 0) {
            target.setDailyLimitPerEmail(dto.getDailyLimitPerEmail());
        }

        // 若填写的不是掩码 ****** 且非空，则更新密码
        if (dto.getPassword() != null && !dto.getPassword().isBlank() && !"******".equals(dto.getPassword().trim())) {
            target.setPassword(dto.getPassword().trim());
        }
    }
}
