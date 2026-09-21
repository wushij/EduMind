package com.edumind.system.api;

import java.util.Collection;
import java.util.Set;

/**
 * 用户偏好跨模块只读查询 API
 */
public interface UserPreferenceQueryApi {

    /**
     * 用户是否启用消息通知（未设置时默认 true）
     */
    boolean isNotificationEnabled(Long userId);

    /**
     * 批量过滤出已启用消息通知的用户 ID（单次批量查询）。
     * 批量下发场景请优先使用本方法，替代循环内逐个调用 {@link #isNotificationEnabled(Long)}，避免 N+1。
     */
    Set<Long> filterNotificationEnabled(Collection<Long> userIds);
}
