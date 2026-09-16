package com.edumind.system.api;

/**
 * 用户偏好跨模块只读查询 API
 */
public interface UserPreferenceQueryApi {

    /**
     * 用户是否启用消息通知（未设置时默认 true）
     */
    boolean isNotificationEnabled(Long userId);
}
