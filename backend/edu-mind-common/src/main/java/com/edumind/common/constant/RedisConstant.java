package com.edumind.common.constant;

public final class RedisConstant {

    public static final String PREFIX = "edumind:";
    public static final String CAPTCHA_KEY = PREFIX + "captcha:";
    public static final String NONCE_KEY = PREFIX + "security:nonce:";
    public static final String RBAC_PERM_KEY = PREFIX + "rbac:perm:";
    public static final String RBAC_ROLE_KEY = PREFIX + "rbac:role:";
    public static final String RATE_LOGIN_KEY = PREFIX + "rate:login:";
    public static final String RATE_REGISTER_KEY = PREFIX + "rate:register:";
    public static final String RATE_CAPTCHA_KEY = PREFIX + "rate:captcha:";
    public static final String RATE_AI_CHAT_KEY = PREFIX + "rate:ai:chat:";
    public static final String RATE_AI_QUESTION_KEY = PREFIX + "rate:ai:question:";
    public static final String RATE_AI_EXAM_KEY = PREFIX + "rate:ai:exam:";
    public static final String AI_SESSION_KEY = PREFIX + "ai:session:";
    public static final String AI_GENERATING_KEY = PREFIX + "ai:generating:";
    public static final String AI_QUOTA_KEY = PREFIX + "ai:quota:";
    public static final String DASHBOARD_KEY = PREFIX + "stats:dashboard:";
    public static final String LOCK_KEY = PREFIX + "lock:";
    public static final String EMAIL_CODE_KEY = PREFIX + "email:code:";
    public static final String EMAIL_LIMIT_KEY = PREFIX + "email:limit:";
    public static final String EMAIL_DAILY_KEY = PREFIX + "email:daily:";
    public static final String EMAIL_RESET_TICKET_KEY = PREFIX + "email:reset_ticket:";
    public static final String CONFIG_CACHE_KEY = PREFIX + "sys:config:";

    public static final long CAPTCHA_TTL_SECONDS = 300L;
    public static final long NONCE_TTL_SECONDS = 300L;
    public static final long RBAC_TTL_SECONDS = 1800L;
    public static final long RATE_WINDOW_SECONDS = 60L;
    public static final long AI_SESSION_TTL_SECONDS = 7200L;
    public static final long DASHBOARD_TTL_SECONDS = 300L;
    public static final long AI_QUOTA_TTL_SECONDS = 86400L;
    public static final long EMAIL_RESET_TICKET_TTL_SECONDS = 600L;

    private RedisConstant() {
    }
}
