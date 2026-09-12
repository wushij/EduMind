package com.edumind.common.api;

import com.edumind.common.utils.JsonUtil;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter / Servlet 层统一写出标准 {@link ApiResult} JSON。
 */
public final class ApiResponseWriter {

    private ApiResponseWriter() {
    }

    public static void write(HttpServletResponse response, int httpStatus, ApiResult<?> body) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtil.toJsonString(body));
    }

    public static void write(HttpServletResponse response, ResultCode resultCode) throws IOException {
        write(response, resolveHttpStatus(resultCode.getCode()), ApiResult.failed(resultCode));
    }

    public static void write(HttpServletResponse response, ResultCode resultCode, String message) throws IOException {
        write(response, resolveHttpStatus(resultCode.getCode()), ApiResult.failed(resultCode.getCode(), message));
    }

    public static void write(HttpServletResponse response, int httpStatus, Integer code, String message) throws IOException {
        write(response, httpStatus, ApiResult.failed(code, message));
    }

    public static int resolveHttpStatus(int businessCode) {
        return switch (businessCode) {
            case 400 -> HttpServletResponse.SC_BAD_REQUEST;
            case 401 -> HttpServletResponse.SC_UNAUTHORIZED;
            case 403 -> HttpServletResponse.SC_FORBIDDEN;
            case 404 -> HttpServletResponse.SC_NOT_FOUND;
            case 405 -> HttpServletResponse.SC_METHOD_NOT_ALLOWED;
            case 429 -> 429;
            case 500 -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            default -> HttpServletResponse.SC_OK;
        };
    }
}
