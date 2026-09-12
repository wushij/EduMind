package com.edumind.security.context;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;

/**
 * 统一解析当前登录用户 ID，兼容 UserContext 与 Sa-Token 两种上下文来源。
 */
public final class LoginUserResolver {

    private LoginUserResolver() {
    }

    public static Long resolveUserId() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            return userId;
        }
        if (StpUtil.isLogin()) {
            try {
                return StpUtil.getLoginIdAsLong();
            } catch (Exception ignored) {
                // fall through
            }
        }
        return null;
    }

    public static Long requireUserId() {
        Long userId = resolveUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return userId;
    }
}
