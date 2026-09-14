package com.edumind.security.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全请求头解析：优先 Header，GET/SSE 等场景可回退 Query 参数。
 */
final class SecurityRequestSupport {

    private SecurityRequestSupport() {
    }

    static String resolveValue(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            value = request.getParameter(name);
        }
        return value;
    }
}
