package com.edumind.system.service.query;

import java.util.Collection;
import java.util.Set;

public interface UserPreferenceQueryService {

    boolean isNotificationEnabled(Long userId);

    /**
     * 批量过滤出已启用消息通知的用户 ID（单次批量查询）。
     * 用于替代循环内逐个调用 {@link #isNotificationEnabled(Long)}，消除 N+1。
     */
    Set<Long> filterNotificationEnabled(Collection<Long> userIds);
}
