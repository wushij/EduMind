package com.edumind.common.exception;

import com.edumind.common.api.ResultCode;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    private final Integer code;

    public AuthException(String message) {
        super(message);
        this.code = ResultCode.UNAUTHORIZED.getCode();
    }
}