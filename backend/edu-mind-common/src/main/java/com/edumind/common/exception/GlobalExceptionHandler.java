package com.edumind.common.exception;

import com.edumind.common.api.ApiResponseWriter;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.ResultCode;
import com.edumind.common.constant.CommonConstant;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: traceId={}, code={}, message={}", traceId(), e.getCode(), e.getMessage());
        return ApiResponseWriter.toResponseEntity(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResult<Void>> handleAuthException(AuthException e) {
        log.warn("Auth exception: traceId={}, message={}", traceId(), e.getMessage());
        return ApiResponseWriter.toResponseEntity(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public ResponseEntity<ApiResult<Void>> handleValidationException(Exception e) {
        String message = extractValidationMessage(e);
        log.warn("Validation failed: traceId={}, message={}", traceId(), message);
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(ResultCode.VALIDATE_FAILED.getMessage());
        log.warn("Constraint violation: traceId={}, message={}", traceId(), message);
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParameterException(MissingServletRequestParameterException e) {
        String message = "缺少必要参数: " + e.getParameterName();
        log.warn("Missing parameter: traceId={}, message={}", traceId(), message);
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = "参数类型错误: " + e.getName();
        log.warn("Type mismatch: traceId={}, message={}", traceId(), message);
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: traceId={}, detail={}", traceId(), e.getMessage());
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, "请求体格式不正确");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        String message = "不支持的 Content-Type: " + e.getContentType();
        log.warn("Unsupported media type: traceId={}, message={}", traceId(), message);
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: traceId={}, method={}", traceId(), e.getMethod());
        return ApiResponseWriter.toResponseEntity(ResultCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler({
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public ResponseEntity<ApiResult<Void>> handleResourceNotFoundException(Exception e) {
        String path = e instanceof NoResourceFoundException resourceNotFound
                ? resourceNotFound.getResourcePath()
                : e.getMessage();
        log.warn("Resource not found: traceId={}, path={}", traceId(), path);
        return ApiResponseWriter.toResponseEntity(ResultCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded: traceId={}", traceId());
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, "上传文件大小超出限制");
    }

    @ExceptionHandler({
            DuplicateKeyException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiResult<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String message = extractDataIntegrityMessage(e);
        log.warn("Data integrity violation: traceId={}, message={}", traceId(), message);
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: traceId={}, message={}", traceId(), e.getMessage());
        return ApiResponseWriter.toResponseEntity(ResultCode.VALIDATE_FAILED, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception e) {
        log.error("Unhandled exception: traceId={}", traceId(), e);
        return ApiResponseWriter.toResponseEntity(ResultCode.FAILED);
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

    private String extractDataIntegrityMessage(DataIntegrityViolationException e) {
        if (e instanceof DuplicateKeyException) {
            return "数据已存在，请勿重复提交";
        }
        Throwable cause = e.getMostSpecificCause();
        String detail = cause != null ? cause.getMessage() : e.getMessage();
        if (detail != null) {
            String lower = detail.toLowerCase();
            if (lower.contains("duplicate entry") || lower.contains("duplicate key") || lower.contains("unique constraint")) {
                return "数据已存在，请勿重复提交";
            }
        }
        return "数据操作冲突，请检查后重试";
    }

    private String traceId() {
        return MDC.get(CommonConstant.TRACE_ID);
    }
}
