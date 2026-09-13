package com.edumind.infrastructure.redis;

import com.edumind.common.constant.RedisConstant;
import com.edumind.common.context.TenantContext;

public final class RedisKeyBuilder {

    private RedisKeyBuilder() {
    }

    /**
     * 构建租户隔离的业务 Key：
     * 若当前存在租户上下文 (或显式指定租户)，生成：edumind:tenant:{tenantId}:{suffix}
     * 否则生成全局兼容 Key：edumind:{suffix}
     */
    public static String tenantScoped(Long tenantId, String suffix) {
        Long tid = tenantId != null ? tenantId : TenantContext.getTenantId();
        if (tid != null && tid > 0) {
            return RedisConstant.PREFIX + "tenant:" + tid + ":" + suffix;
        }
        return RedisConstant.PREFIX + suffix;
    }

    // --- 全局 / 平台级 Key (不加 tenant) ---

    public static String captcha(String id) {
        return RedisConstant.CAPTCHA_KEY + id;
    }

    public static String nonce(String nonce) {
        return RedisConstant.NONCE_KEY + nonce;
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

    public static String emailCode(String scene, String email) {
        return RedisConstant.EMAIL_CODE_KEY + scene.toLowerCase().trim() + ":" + email.toLowerCase().trim();
    }

    public static String emailLimit(String scene, String email) {
        return RedisConstant.EMAIL_LIMIT_KEY + scene.toLowerCase().trim() + ":" + email.toLowerCase().trim();
    }

    public static String emailDaily(String email, String date) {
        return RedisConstant.EMAIL_DAILY_KEY + email.toLowerCase().trim() + ":" + date;
    }

    public static String emailResetTicket(String ticket) {
        return RedisConstant.EMAIL_RESET_TICKET_KEY + ticket.trim();
    }

    public static String sysConfig(String key) {
        return RedisConstant.CONFIG_CACHE_KEY + key.trim();
    }

    // --- 租户业务级 Key (多租户隔离) ---

    public static String rbacPermissions(Long userId) {
        return rbacPermissions(null, userId);
    }

    public static String rbacPermissions(Long tenantId, Long userId) {
        return tenantScoped(tenantId, "rbac:perm:" + userId);
    }

    public static String rbacRoles(Long userId) {
        return rbacRoles(null, userId);
    }

    public static String rbacRoles(Long tenantId, Long userId) {
        return tenantScoped(tenantId, "rbac:role:" + userId);
    }

    public static String rateAiChat(Long userId) {
        return rateAiChat(null, userId);
    }

    public static String rateAiChat(Long tenantId, Long userId) {
        return tenantScoped(tenantId, "rate:ai:chat:" + userId);
    }

    public static String rateAiQuestion(Long userId) {
        return rateAiQuestion(null, userId);
    }

    public static String rateAiQuestion(Long tenantId, Long userId) {
        return tenantScoped(tenantId, "rate:ai:question:" + userId);
    }

    public static String rateAiExam(Long userId) {
        return rateAiExam(null, userId);
    }

    public static String rateAiExam(Long tenantId, Long userId) {
        return tenantScoped(tenantId, "rate:ai:exam:" + userId);
    }

    public static String aiSession(String conversationId) {
        return aiSession(null, conversationId);
    }

    public static String aiSession(Long tenantId, String conversationId) {
        return tenantScoped(tenantId, "ai:session:" + conversationId);
    }

    public static String aiGenerating(String biz, Long userId) {
        return aiGenerating(null, biz, userId);
    }

    public static String aiGenerating(Long tenantId, String biz, Long userId) {
        return tenantScoped(tenantId, "ai:generating:" + biz + ":" + userId);
    }

    public static String aiQuota(Long userId, String date) {
        return aiQuota(null, userId, date);
    }

    public static String aiQuota(Long tenantId, Long userId, String date) {
        return tenantScoped(tenantId, "ai:quota:" + userId + ":" + date);
    }

    public static String dashboard(Long userId) {
        return dashboard(null, userId);
    }

    public static String dashboard(Long tenantId, Long userId) {
        return tenantScoped(tenantId, "stats:dashboard:" + userId);
    }

    public static String lock(String biz, String id) {
        return lock(null, biz, id);
    }

    public static String lock(Long tenantId, String biz, String id) {
        return tenantScoped(tenantId, "lock:" + biz + ":" + id);
    }
}
