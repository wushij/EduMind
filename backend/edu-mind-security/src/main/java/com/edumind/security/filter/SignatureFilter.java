package com.edumind.security.filter;

import com.edumind.common.api.ApiResponseWriter;
import com.edumind.common.api.ResultCode;
import com.edumind.security.config.DynamicSecurityConfigService;
import com.edumind.security.config.SecurityProperties;
import com.edumind.security.crypto.SignatureService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 接口签名防篡改过滤器
 * 基于国密 SM3-HMAC 算法验证请求签名
 */
@Component
@Order(-90)
@RequiredArgsConstructor
public class SignatureFilter extends OncePerRequestFilter {

    private final SignatureService signatureService;
    private final SecurityProperties securityProperties;
    private final DynamicSecurityConfigService dynamicSecurityConfigService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean smRequired = (securityProperties.isSmEnabled() || dynamicSecurityConfigService.isSm3SignEnabled()) && isSensitivePath(uri);
        String signatureHeader = request.getHeader("X-Signature");

        if (!smRequired && signatureHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest wrapped = request;
        String body = "";
        if ("POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())
                || "PATCH".equalsIgnoreCase(request.getMethod())) {
            wrapped = new CachedBodyHttpServletRequest(request);
            body = ((CachedBodyHttpServletRequest) wrapped).getBodyString();
        }

        if (signatureHeader == null) {
            ApiResponseWriter.write(response, ResultCode.FORBIDDEN, "国密模式已开启：缺少 X-Signature");
            return;
        }

        String timestampHeader = request.getHeader("X-Timestamp");
        String nonceHeader = request.getHeader("X-Nonce");
        if (timestampHeader == null || nonceHeader == null) {
            ApiResponseWriter.write(response, ResultCode.VALIDATE_FAILED, "签名校验失败：缺少 X-Timestamp 或 X-Nonce");
            return;
        }

        long timestamp = Long.parseLong(timestampHeader);
        String path = request.getRequestURI();
        String method = request.getMethod();
        String secretKey = securityProperties.getHmacSecret();

        boolean isValid = signatureService.verifySignature(method, path, timestamp, nonceHeader, body, secretKey, signatureHeader);
        if (!isValid) {
            ApiResponseWriter.write(response, ResultCode.FORBIDDEN, "请求签名验证未通过，内容可能已被篡改");
            return;
        }

        filterChain.doFilter(wrapped, response);
    }

    private boolean isSensitivePath(String uri) {
        return securityProperties.getSensitivePaths().stream().anyMatch(uri::startsWith);
    }
}
