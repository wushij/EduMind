package com.edumind.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleCode {
    ADMIN("ADMIN"),
    TENANT_ADMIN("TENANT_ADMIN"),
    ORG_ADMIN("ORG_ADMIN"),
    TEACHER("TEACHER"),
    STUDENT("STUDENT");

    private final String code;
}
