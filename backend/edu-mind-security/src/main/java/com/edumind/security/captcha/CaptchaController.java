package com.edumind.security.captcha;

import com.edumind.common.api.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码统一接口 (支持字符图形验证码与拼图滑块验证码)
 */
@RestController
@RequestMapping("/api/auth/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;
    private final SliderCaptchaService sliderCaptchaService;

    /**
     * 获取字符图形验证码 (LineCaptcha 130×48 纯白底色)
     */
    @GetMapping
    public ApiResult<CaptchaVO> getCaptcha(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        return ApiResult.success(captchaService.create());
    }

    /**
     * 获取系统验证码公开策略 (图形验证码 vs 滑块验证码)
     */
    @GetMapping("/policy")
    public ApiResult<CaptchaPolicyVO> getCaptchaPolicy() {
        return ApiResult.success(sliderCaptchaService.getCaptchaPolicy());
    }

    /**
     * 生成滑块拼图挑战 (返回 320×160 背景图与拼图块 Base64)
     */
    @PostMapping("/slider/challenge")
    public ApiResult<SliderChallengeVO> createSliderChallenge(
            @RequestBody(required = false) SliderChallengeDTO dto,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest request,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        String clientIp = resolveClientIp(request);
        return ApiResult.success(sliderCaptchaService.renderChallenge(dto, clientIp, deviceId));
    }

    /**
     * 校验滑块拖动轨迹并签发凭证 captcha_token
     */
    @PostMapping("/slider/verify")
    public ApiResult<SliderVerifyVO> verifySliderChallenge(
            @Valid @RequestBody SliderVerifyDTO dto,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        return ApiResult.success(sliderCaptchaService.verifyChallenge(dto, clientIp, deviceId));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "";
    }
}