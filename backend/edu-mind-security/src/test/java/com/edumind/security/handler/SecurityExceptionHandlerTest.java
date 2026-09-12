package com.edumind.security.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.ResultCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SecurityExceptionHandlerTest {

    private final SecurityExceptionHandler handler = new SecurityExceptionHandler();

    @Test
    void shouldReturnStandardUnauthorizedPayload() {
        NotLoginException exception = NotLoginException.newInstance(
                NotLoginException.NOT_TOKEN,
                null,
                null,
                "token missing");

        ApiResult<Void> result = handler.handleNotLoginException(exception);

        Assertions.assertEquals(ResultCode.UNAUTHORIZED.getCode(), result.getCode());
        Assertions.assertEquals(ResultCode.UNAUTHORIZED.getMessage(), result.getMessage());
        Assertions.assertNotNull(result.getTimestamp());
    }

    @Test
    void shouldReturnStandardForbiddenPayload() {
        ApiResult<Void> result = handler.handleNotPermissionException(
                new NotPermissionException("ai:question", null));

        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), result.getCode());
        Assertions.assertEquals(ResultCode.FORBIDDEN.getMessage(), result.getMessage());
    }
}
