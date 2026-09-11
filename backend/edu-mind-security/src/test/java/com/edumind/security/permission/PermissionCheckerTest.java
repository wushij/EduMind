package com.edumind.security.permission;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PermissionCheckerTest {

    private final PermissionChecker permissionChecker = new PermissionChecker();

    @Test
    void shouldReturnFalseWhenNotLoggedIn() {
        Assertions.assertFalse(permissionChecker.hasPermission("system:user:edit"));
    }
}
