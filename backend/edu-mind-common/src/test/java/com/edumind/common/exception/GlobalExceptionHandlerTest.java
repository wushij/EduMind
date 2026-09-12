package com.edumind.common.exception;

import com.edumind.common.api.ApiResult;
import com.edumind.common.api.ResultCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnStandardBusinessExceptionPayload() {
        ApiResult<Void> result = handler.handleBusinessException(new BusinessException("课程不存在"));

        Assertions.assertEquals(ResultCode.FAILED.getCode(), result.getCode());
        Assertions.assertEquals("课程不存在", result.getMessage());
        Assertions.assertNull(result.getData());
        Assertions.assertNotNull(result.getTimestamp());
    }

    @Test
    void shouldReturnStandardAuthExceptionPayload() {
        ApiResult<Void> result = handler.handleAuthException(new AuthException("登录已过期"));

        Assertions.assertEquals(ResultCode.UNAUTHORIZED.getCode(), result.getCode());
        Assertions.assertEquals("登录已过期", result.getMessage());
    }

    @Test
    void shouldReturnFieldValidationMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "username", "用户名不能为空"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ApiResult<Void> result = handler.handleValidationException(exception);

        Assertions.assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        Assertions.assertEquals("username: 用户名不能为空", result.getMessage());
    }

    @Test
    void shouldHideInternalErrorMessage() {
        ApiResult<Void> result = handler.handleException(new RuntimeException("database password leaked"));

        Assertions.assertEquals(ResultCode.FAILED.getCode(), result.getCode());
        Assertions.assertEquals(ResultCode.FAILED.getMessage(), result.getMessage());
        Assertions.assertFalse(result.getMessage().contains("database"));
    }

    @Test
    void shouldReturnMalformedBodyMessage() {
        ApiResult<Void> result = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("invalid json", null, null));

        Assertions.assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        Assertions.assertEquals("请求体格式不正确", result.getMessage());
    }
}
