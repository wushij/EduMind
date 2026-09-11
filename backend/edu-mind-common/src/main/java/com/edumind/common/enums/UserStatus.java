package com.edumind.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
    NORMAL(1, "正常"),
    LOCKED(2, "锁定"),
    DISABLED(3, "禁用");

    private final Integer code;
    private final String desc;
}