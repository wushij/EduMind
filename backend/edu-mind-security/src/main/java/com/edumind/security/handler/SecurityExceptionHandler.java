package com.edumind.security.handler;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 安全与鉴权异常统一处理器
 */
@Slf4j
@Order(-100)
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleNotLoginException(NotLoginException e) {
        log.warn("Sa-Token not login: type={}, message={}", e.getType(), e.getMessage());
        return ApiResult.failed(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("Sa-Token permission denied: permission={}", e.getPermission());
        return ApiResult.failed(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
    }

    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleNotRoleException(NotRoleException e) {
        log.warn("Sa-Token role denied: role={}", e.getRole());
        return ApiResult.failed(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
    }

    @ExceptionHandler(DisableServiceException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleDisableServiceException(DisableServiceException e) {
        log.warn("Sa-Token service disabled: service={}, level={}", e.getService(), e.getLevel());
        return ApiResult.failed(ResultCode.FORBIDDEN.getCode(), "账号服务已被禁用，请联系管理员");
    }

    @ExceptionHandler(SaTokenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleSaTokenException(SaTokenException e) {
        log.warn("Sa-Token exception: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResult.failed(ResultCode.FORBIDDEN.getCode(), e.getMessage());
    }
}
