package com.edumind.security.filter;

import com.edumind.infrastructure.redis.RedisService;
import com.edumind.infrastructure.redis.RedisSupport;
import com.edumind.security.config.DynamicSecurityConfigService;
import com.edumind.security.config.SecurityProperties;
import com.edumind.security.crypto.SignatureService;
import com.edumind.security.crypto.Sm3HmacService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilterSecurityTest {

    private ReplayAttackFilter replayAttackFilter;
    private SignatureFilter signatureFilter;
    private SignatureService signatureService;
    private DynamicSecurityConfigService dynamicSecurityConfigService;

    @BeforeEach
    public void setUp() {
        RedisService redisService = mock(RedisService.class);
        RedisSupport redisSupport = mock(RedisSupport.class);
        when(redisSupport.useRedisOrFallback()).thenReturn(false);
        when(redisSupport.requireRedis()).thenReturn(false);
        SecurityProperties securityProperties = new SecurityProperties();
        dynamicSecurityConfigService = mock(DynamicSecurityConfigService.class);
        when(dynamicSecurityConfigService.isSm3SignEnabled()).thenReturn(false);
        when(dynamicSecurityConfigService.isTimestampEnabled()).thenReturn(false);
        when(dynamicSecurityConfigService.isNonceEnabled()).thenReturn(false);
        when(dynamicSecurityConfigService.getTimestampWindowMs()).thenReturn(300000L);
        replayAttackFilter = new ReplayAttackFilter(redisService, redisSupport, securityProperties, dynamicSecurityConfigService);
        Sm3HmacService sm3HmacService = new Sm3HmacService();
        signatureService = new SignatureService(sm3HmacService);
        signatureFilter = new SignatureFilter(signatureService, securityProperties, dynamicSecurityConfigService);
    }

    @Test
    public void testReplayAttackFilter_Success() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/course/list");
        long now = System.currentTimeMillis();
        request.addHeader("X-Timestamp", String.valueOf(now));
        request.addHeader("X-Nonce", "nonce_valid_001");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        replayAttackFilter.doFilter(request, response, filterChain);

        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    public void testReplayAttackFilter_TimestampExpired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/course/list");
        long expiredTime = System.currentTimeMillis() - (6 * 60 * 1000L);
        request.addHeader("X-Timestamp", String.valueOf(expiredTime));
        request.addHeader("X-Nonce", "nonce_expired_002");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        replayAttackFilter.doFilter(request, response, filterChain);

        Assertions.assertEquals(403, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("时间戳超出允许范围"));
    }

    @Test
    public void testReplayAttackFilter_DuplicateNonceBlocked() throws Exception {
        long now = System.currentTimeMillis();
        String nonce = "replay_nonce_test_003";

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/ai/generate");
        request1.addHeader("X-Timestamp", String.valueOf(now));
        request1.addHeader("X-Nonce", nonce);
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        replayAttackFilter.doFilter(request1, response1, new MockFilterChain());
        Assertions.assertEquals(200, response1.getStatus());

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/ai/generate");
        request2.addHeader("X-Timestamp", String.valueOf(now));
        request2.addHeader("X-Nonce", nonce);
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        replayAttackFilter.doFilter(request2, response2, new MockFilterChain());

        Assertions.assertEquals(403, response2.getStatus());
        Assertions.assertTrue(response2.getContentAsString().contains("检测到重放攻击：Nonce 已被使用"));
    }

    @Test
    public void testSignatureFilter_SuccessAndTamperDetection() throws Exception {
        long now = System.currentTimeMillis();
        String nonce = "sign_nonce_004";
        String path = "/api/v1/ai/chat";
        String method = "GET";
        String secretKey = "EduMind_Platform_SecretKey_2026";

        String signature = signatureService.generateSignature(method, path, now, nonce, "", secretKey);

        MockHttpServletRequest requestValid = new MockHttpServletRequest(method, path);
        requestValid.addHeader("X-Timestamp", String.valueOf(now));
        requestValid.addHeader("X-Nonce", nonce);
        requestValid.addHeader("X-Signature", signature);
        MockHttpServletResponse responseValid = new MockHttpServletResponse();

        signatureFilter.doFilter(requestValid, responseValid, new MockFilterChain());
        Assertions.assertEquals(200, responseValid.getStatus());

        MockHttpServletRequest requestTampered = new MockHttpServletRequest(method, "/api/v1/ai/other");
        requestTampered.addHeader("X-Timestamp", String.valueOf(now));
        requestTampered.addHeader("X-Nonce", nonce);
        requestTampered.addHeader("X-Signature", signature);
        MockHttpServletResponse responseTampered = new MockHttpServletResponse();

        signatureFilter.doFilter(requestTampered, responseTampered, new MockFilterChain());
        Assertions.assertEquals(403, responseTampered.getStatus());
        Assertions.assertTrue(responseTampered.getContentAsString().contains("请求签名验证未通过"));
    }

    @Test
    public void testSignatureFilter_SmModeMissingSignatureOnSensitivePath() throws Exception {
        SecurityProperties props = new SecurityProperties();
        props.setSmEnabled(true);
        props.setSensitivePaths(List.of("/api/analytics"));
        props.setHmacSecret("EduMind_Platform_SecretKey_2026");
        SignatureFilter smFilter = new SignatureFilter(signatureService, props, dynamicSecurityConfigService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/analytics/learning");
        MockHttpServletResponse response = new MockHttpServletResponse();
        smFilter.doFilter(request, response, new MockFilterChain());

        Assertions.assertEquals(403, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("缺少 X-Signature"));
    }

    @Test
    public void testSignatureFilter_PostBodyTamperRejected() throws Exception {
        SecurityProperties props = new SecurityProperties();
        props.setSmEnabled(true);
        props.setSensitivePaths(List.of("/api/ai/agent"));
        props.setHmacSecret("EduMind_Platform_SecretKey_2026");
        SignatureFilter smFilter = new SignatureFilter(signatureService, props, dynamicSecurityConfigService);

        long now = System.currentTimeMillis();
        String nonce = "body_tamper_nonce";
        String path = "/api/ai/agent/runs";
        String body = "{\"agentCode\":\"teaching\"}";
        String signature = signatureService.generateSignature("POST", path, now, nonce, body, props.getHmacSecret());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContent("{\"agentCode\":\"grading\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Timestamp", String.valueOf(now));
        request.addHeader("X-Nonce", nonce);
        request.addHeader("X-Signature", signature);
        MockHttpServletResponse response = new MockHttpServletResponse();

        smFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(403, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("请求签名验证未通过"));
    }
}
