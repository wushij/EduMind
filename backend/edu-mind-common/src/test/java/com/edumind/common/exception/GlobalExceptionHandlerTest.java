package com.edumind.common.exception;

import com.edumind.common.api.ApiResult;
import com.edumind.common.api.ResultCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnStandardBusinessExceptionPayload() {
        ResponseEntity<ApiResult<Void>> response = handler.handleBusinessException(new BusinessException("课程不存在"));

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertEquals(ResultCode.FAILED.getCode(), response.getBody().getCode());
        Assertions.assertEquals("课程不存在", response.getBody().getMessage());
        Assertions.assertNull(response.getBody().getData());
        Assertions.assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void shouldAlignBusinessExceptionHttpStatusWithBusinessCode() {
        ResponseEntity<ApiResult<Void>> forbidden = handler.handleBusinessException(
                new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问"));
        ResponseEntity<ApiResult<Void>> notFound = handler.handleBusinessException(
                new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "资源不存在"));

        Assertions.assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), forbidden.getBody().getCode());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        Assertions.assertEquals(ResultCode.RESOURCE_NOT_FOUND.getCode(), notFound.getBody().getCode());
    }

    @Test
    void shouldReturnStandardAuthExceptionPayload() {
        ResponseEntity<ApiResult<Void>> response = handler.handleAuthException(new AuthException("登录已过期"));

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Assertions.assertEquals(ResultCode.UNAUTHORIZED.getCode(), response.getBody().getCode());
        Assertions.assertEquals("登录已过期", response.getBody().getMessage());
    }

    @Test
    void shouldReturnFieldValidationMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "username", "用户名不能为空"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResult<Void>> response = handler.handleValidationException(exception);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals(ResultCode.VALIDATE_FAILED.getCode(), response.getBody().getCode());
        Assertions.assertEquals("username: 用户名不能为空", response.getBody().getMessage());
    }

    @Test
    void shouldHideInternalErrorMessage() {
        ResponseEntity<ApiResult<Void>> response = handler.handleException(new RuntimeException("database password leaked"));

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertEquals(ResultCode.FAILED.getCode(), response.getBody().getCode());
        Assertions.assertEquals(ResultCode.FAILED.getMessage(), response.getBody().getMessage());
        Assertions.assertFalse(response.getBody().getMessage().contains("database"));
    }

    @Test
    void shouldReturnMalformedBodyMessage() {
        ResponseEntity<ApiResult<Void>> response = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("invalid json", null, null));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals(ResultCode.VALIDATE_FAILED.getCode(), response.getBody().getCode());
        Assertions.assertEquals("请求体格式不正确", response.getBody().getMessage());
    }

    @Test
    void shouldReturnNotFoundForMissingResource() {
        ResponseEntity<ApiResult<Void>> response = handler.handleResourceNotFoundException(
                new NoResourceFoundException(HttpMethod.GET, "/api/missing"));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Assertions.assertEquals(ResultCode.RESOURCE_NOT_FOUND.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldReturnUploadSizeLimitMessage() {
        ResponseEntity<ApiResult<Void>> response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(1024L));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("上传文件大小超出限制", response.getBody().getMessage());
    }

    @Test
    void shouldReturnDuplicateKeyMessage() {
        ResponseEntity<ApiResult<Void>> response = handler.handleDataIntegrityViolationException(
                new DuplicateKeyException("Duplicate entry '1' for key 'uk_user_email'"));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("数据已存在，请勿重复提交", response.getBody().getMessage());
    }
}
