package com.edumind.security.permission;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

@Component
public class PermissionChecker {

    public boolean hasPermission(String permission) {
        try {
            if (!StpUtil.isLogin()) {
                return false;
            }
            return StpUtil.hasPermission(permission);
        } catch (Exception ex) {
            return false;
        }
    }
}