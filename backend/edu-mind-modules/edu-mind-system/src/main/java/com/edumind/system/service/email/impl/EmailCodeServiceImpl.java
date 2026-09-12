package com.edumind.system.service.email.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.mail.MailClient;
import com.edumind.infrastructure.mail.MailConfig;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.security.captcha.CaptchaService;
import com.edumind.system.dto.auth.EmailSendCodeDTO;
import com.edumind.system.service.config.SysConfigService;
import com.edumind.system.service.email.EmailCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {

    private final SysConfigService sysConfigService;
    private final MailClient mailClient;
    private final RedisService redisService;
    private final CaptchaService captchaService;

    @Override
    public void sendCode(EmailSendCodeDTO dto) {
        String email = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱地址不能为空");
        }

        // 若携带图形验证码则执行防刷校验
        if (StringUtils.hasText(dto.getCaptchaId()) || StringUtils.hasText(dto.getCaptcha())) {
            captchaService.verify(dto.getCaptchaId(), dto.getCaptcha());
        }

        String scene = normalizeScene(dto.getScene());
        MailConfig mailConfig = sysConfigService.getMailConfig();

        // 1. 发送冷却校验（防短时间连续狂刷）
        String limitKey = RedisKeyBuilder.emailLimit(scene, email);
        if (redisService.get(limitKey) != null) {
            int interval = mailConfig.getCodeIntervalSeconds() != null ? mailConfig.getCodeIntervalSeconds() : 60;
            throw new BusinessException(String.format("验证码发送过于频繁，请 %d 秒后再试", interval));
        }

        // 2. 单邮箱每日发送次数上限校验（防针对特定邮箱邮件轰炸）
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String dailyKey = RedisKeyBuilder.emailDaily(email, today);
        int maxDaily = mailConfig.getDailyLimitPerEmail() != null ? mailConfig.getDailyLimitPerEmail() : 10;
        String currentDailyStr = redisService.get(dailyKey);
        int currentDaily = 0;
        if (currentDailyStr != null) {
            try {
                currentDaily = Integer.parseInt(currentDailyStr);
            } catch (NumberFormatException ignored) {
            }
        }
        if (currentDaily >= maxDaily) {
            throw new BusinessException(String.format("该邮箱今日验证码发送已达上限（%d次），请明日再试", maxDaily));
        }

        // 3. 生成 6 位纯数字专属安全验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        int expireMinutes = mailConfig.getCodeExpireMinutes() != null ? mailConfig.getCodeExpireMinutes() : 5;
        int intervalSeconds = mailConfig.getCodeIntervalSeconds() != null ? mailConfig.getCodeIntervalSeconds() : 60;

        // 4. Redis 写入凭据与频控标记
        String codeKey = RedisKeyBuilder.emailCode(scene, email);
        redisService.set(codeKey, code, (long) expireMinutes * 60);
        redisService.set(limitKey, "1", intervalSeconds);

        long newDaily = redisService.increment(dailyKey, 86400);
        log.info("生成邮箱验证码成功 [email={}, scene={}, dailyCount={}, code={}]", email, scene, newDaily, code);

        // 5. 异步投递富文本双通道防进垃圾箱邮件
        mailClient.sendVerificationCodeAsync(mailConfig, email, scene, code, expireMinutes);
    }

    @Override
    public void verifyCode(String email, String scene, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            throw new BusinessException("邮箱和验证码不能为空");
        }
        String cleanEmail = email.trim().toLowerCase();
        String cleanCode = code.trim();
        String cleanScene = normalizeScene(scene);

        String codeKey = RedisKeyBuilder.emailCode(cleanScene, cleanEmail);
        String savedCode = redisService.get(codeKey);

        if (savedCode == null || savedCode.isBlank()) {
            throw new BusinessException("验证码已过期或不存在，请重新获取");
        }

        if (!savedCode.equalsIgnoreCase(cleanCode)) {
            throw new BusinessException("验证码错误");
        }

        // 核验成功即刻单次失效，防止重放攻击
        redisService.delete(codeKey);
    }

    private String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "login";
        }
        String s = scene.trim().toLowerCase();
        return switch (s) {
            case "bind" -> "bind";
            case "resetpwd", "modifypwd" -> "resetpwd";
            default -> "login";
        };
    }
}
