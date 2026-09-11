package com.edumind.security.filter;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String signatureHeader = request.getHeader("X-Signature");

        // 若携带签名头，执行 SM3-HMAC 防篡改签名校验
        if (signatureHeader != null) {
            String timestampHeader = request.getHeader("X-Timestamp");
            String nonceHeader = request.getHeader("X-Nonce");

            if (timestampHeader == null || nonceHeader == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":400,\"message\":\"签名校验失败：缺少 X-Timestamp 或 X-Nonce\"}");
                return;
            }

            // 获取预共享密钥（开发环境或租户系统分配秘钥）
            String secretKey = "EduMind_Platform_SecretKey_2026";
            long timestamp = Long.parseLong(timestampHeader);
            String path = request.getRequestURI();
            String method = request.getMethod();

            boolean isValid = signatureService.verifySignature(method, path, timestamp, nonceHeader, "", secretKey, signatureHeader);
            if (!isValid) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"请求签名验证未通过，内容可能已被篡改\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}