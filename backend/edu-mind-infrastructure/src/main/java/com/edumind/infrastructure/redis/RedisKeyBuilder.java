package com.edumind.infrastructure.redis;

import com.edumind.common.constant.RedisConstant;

public final class RedisKeyBuilder {

    private RedisKeyBuilder() {
    }

    public static String captcha(String id) {
        return RedisConstant.CAPTCHA_KEY + id;
    }

    public static String nonce(String nonce) {
        return RedisConstant.NONCE_KEY + nonce;
    }

    public static String rbacPermissions(Long userId) {
        return RedisConstant.RBAC_PERM_KEY + userId;
    }

    public static String rbacRoles(Long userId) {
        return RedisConstant.RBAC_ROLE_KEY + userId;
    }

    public static String rateLogin(String ip) {
        return RedisConstant.RATE_LOGIN_KEY + ip;
    }

    public static String rateRegister(String ip) {
        return RedisConstant.RATE_REGISTER_KEY + ip;
    }

    public static String rateCaptcha(String ip) {
        return RedisConstant.RATE_CAPTCHA_KEY + ip;
    }

    public static String rateAiChat(Long userId) {
        return RedisConstant.RATE_AI_CHAT_KEY + userId;
    }

    public static String rateAiQuestion(Long userId) {
        return RedisConstant.RATE_AI_QUESTION_KEY + userId;
    }

    public static String rateAiExam(Long userId) {
        return RedisConstant.RATE_AI_EXAM_KEY + userId;
    }

    public static String aiSession(String conversationId) {
        return RedisConstant.AI_SESSION_KEY + conversationId;
    }

    public static String aiGenerating(String biz, Long userId) {
        return RedisConstant.AI_GENERATING_KEY + biz + ":" + userId;
    }

    public static String aiQuota(Long userId, String date) {
        return RedisConstant.AI_QUOTA_KEY + userId + ":" + date;
    }

    public static String dashboard(Long userId) {
        return RedisConstant.DASHBOARD_KEY + userId;
    }

    public static String lock(String biz, String id) {
        return RedisConstant.LOCK_KEY + biz + ":" + id;
    }

    public static String emailCode(String scene, String email) {
        return RedisConstant.EMAIL_CODE_KEY + scene.toLowerCase().trim() + ":" + email.toLowerCase().trim();
    }

    public static String emailLimit(String scene, String email) {
        return RedisConstant.EMAIL_LIMIT_KEY + scene.toLowerCase().trim() + ":" + email.toLowerCase().trim();
    }

    public static String emailDaily(String email, String date) {
        return RedisConstant.EMAIL_DAILY_KEY + email.toLowerCase().trim() + ":" + date;
    }

    public static String sysConfig(String key) {
        return RedisConstant.CONFIG_CACHE_KEY + key.trim();
    }
}
