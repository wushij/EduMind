package com.edumind.common.exception;

import com.edumind.common.api.ApiResult;
import com.edumind.common.api.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResult.failed(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleAuthException(AuthException e) {
        log.warn("Auth exception: {}", e.getMessage());
        return ApiResult.failed(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidationException(Exception e) {
        String message = extractValidationMessage(e);
        log.warn("Validation failed: {}", message);
        return ApiResult.failed(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(ResultCode.VALIDATE_FAILED.getMessage());
        log.warn("Constraint violation: {}", message);
        return ApiResult.failed(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMissingParameterException(MissingServletRequestParameterException e) {
        String message = "缺少必要参数: " + e.getParameterName();
        log.warn(message);
        return ApiResult.failed(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = "参数类型错误: " + e.getName();
        log.warn(message);
        return ApiResult.failed(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return ApiResult.failed(ResultCode.VALIDATE_FAILED.getCode(), "请求体格式不正确");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResult<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMethod());
        return ApiResult.failed(ResultCode.METHOD_NOT_ALLOWED.getCode(), ResultCode.METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ApiResult.failed(ResultCode.VALIDATE_FAILED.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ApiResult.failed(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMessage());
    }

    private String extractValidationMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException validException) {
            return validException.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse(ResultCode.VALIDATE_FAILED.getMessage());
        }
        if (e instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse(ResultCode.VALIDATE_FAILED.getMessage());
        }
        return ResultCode.VALIDATE_FAILED.getMessage();
    }
}
