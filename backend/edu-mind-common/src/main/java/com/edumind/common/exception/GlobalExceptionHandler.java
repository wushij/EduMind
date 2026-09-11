package com.edumind.common.exception;

import com.edumind.common.api.ApiResult;
import com.edumind.common.api.ResultCode;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException e) {
        return ApiResult.failed(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public ApiResult<Void> handleAuthException(AuthException e) {
        return ApiResult.failed(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        String defaultMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResult.failed(ResultCode.VALIDATE_FAILED.getCode(), defaultMessage);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        return ApiResult.failed(ResultCode.FAILED.getCode(), "服务器内部错误: " + e.getMessage());
    }
}