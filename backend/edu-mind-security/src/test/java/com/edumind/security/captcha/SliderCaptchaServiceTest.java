package com.edumind.security.captcha;

import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.infrastructure.redis.RedisSupport;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SliderCaptchaServiceTest {

    private SliderCaptchaService sliderCaptchaService;
    private RedisService redisService;
    private RedisSupport redisSupport;

    @BeforeEach
    void setUp() {
        redisService = Mockito.mock(RedisService.class);
        redisSupport = Mockito.mock(RedisSupport.class);
        // 走本地内存缓存分支测试
        when(redisSupport.useRedisOrFallback()).thenReturn(false);
        sliderCaptchaService = new SliderCaptchaService(redisService, redisSupport);
    }

    @Test
    @DisplayName("读取登录配置时应使用与系统配置一致的 Redis Key")
    void testGetCaptchaPolicy_ReadsLoginConfigFromStandardRedisKey() throws Exception {
        when(redisSupport.useRedisOrFallback()).thenReturn(true);
        String configJson = "{\"captchaEnabled\":true,\"captchaType\":\"image\",\"emailLoginSliderCaptchaEnabled\":false}";
        when(redisService.get(RedisKeyBuilder.sysConfig("sys.login.config"))).thenReturn(configJson);

        CaptchaPolicyVO policy = sliderCaptchaService.getCaptchaPolicy();

        assertEquals("image", policy.getCaptchaType());
        assertTrue(policy.isCaptchaEnabled());
        assertFalse(policy.isEmailLoginSliderCaptchaEnabled());
    }

    @Test
    @DisplayName("测试滑块挑战生成、抠图及底图Base64正确性")
    void testRenderChallenge() {
        SliderChallengeDTO dto = SliderChallengeDTO.builder()
                .operation("LOGIN")
                .username("admin")
                .build();

        SliderChallengeVO vo = sliderCaptchaService.renderChallenge(dto, "127.0.0.1", "device_001");

        assertNotNull(vo);
        assertTrue(vo.getChallengeId().startsWith("sc_"));
        assertEquals(320, vo.getWidth());
        assertEquals(160, vo.getHeight());
        assertTrue(vo.getPieceSize() >= 44 && vo.getPieceSize() <= 52);
        assertTrue(vo.getPieceY() >= 20);
        assertTrue(vo.getBackground().startsWith("data:image/jpeg;base64,"));
        assertTrue(vo.getPiece().startsWith("data:image/png;base64,"));
        assertTrue(vo.getExpireAt() > System.currentTimeMillis());
    }

    @Test
    @DisplayName("测试人机行为轨迹风控评分与滑块校验通过流程")
    void testVerifyChallenge_Success() {
        SliderChallengeDTO dto = SliderChallengeDTO.builder()
                .operation("LOGIN")
                .username("test_user")
                .build();
        SliderChallengeVO challenge = sliderCaptchaService.renderChallenge(dto, "127.0.0.1", "device_001");

        // 模拟人类自然拖动轨迹 (平滑加速、微颤抖、总耗时 1000ms)
        // 从 service 内存中读取对应 challenge
        int targetX = 120;
        try {
            var field = SliderCaptchaService.class.getDeclaredField("localCache");
            field.setAccessible(true);
            java.util.Map<?, ?> cache = (java.util.Map<?, ?>) field.get(sliderCaptchaService);
            Object entry = cache.get("captcha:slider:challenge:" + challenge.getChallengeId());
            if (entry != null) {
                var valField = entry.getClass().getDeclaredField("value");
                valField.setAccessible(true);
                String json = (String) valField.get(entry);
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
                targetX = node.get("targetX").asInt();
            }
        } catch (Exception ignored) {
        }

        List<SliderTrackEvent> events = new ArrayList<>();
        events.add(new SliderTrackEvent(0, 0, 0));
        int steps = 15;
        for (int i = 1; i <= steps; i++) {
            int t = i * 65;
            double progress = (double) i / steps;
            int x = (int) Math.round(targetX * Math.sin(progress * Math.PI / 2));
            int y = (i % 3 == 0) ? 1 : ((i % 3 == 1) ? -1 : 0);
            events.add(new SliderTrackEvent(x, y, t));
        }

        SliderVerifyDTO verifyDTO = SliderVerifyDTO.builder()
                .challengeId(challenge.getChallengeId())
                .offsetX(targetX)
                .durationMs(1000)
                .events(events)
                .username("test_user")
                .build();

        SliderVerifyVO verifyVO = sliderCaptchaService.verifyChallenge(verifyDTO, "127.0.0.1", "device_001");
        assertNotNull(verifyVO);
        assertTrue(verifyVO.getCaptchaToken().startsWith("ct_slider_"));
        assertTrue(verifyVO.getExpireAt() > System.currentTimeMillis());

        // 验证一次性消费成功
        assertDoesNotThrow(() ->
                sliderCaptchaService.consumeToken(verifyVO.getCaptchaToken(), "test_user", "LOGIN", "device_001")
        );

        // 重复消费必须抛出异常防重放
        assertThrows(BusinessException.class, () ->
                sliderCaptchaService.consumeToken(verifyVO.getCaptchaToken(), "test_user", "LOGIN", "device_001")
        );
    }

    @Test
    @DisplayName("测试位置偏差过大时校验失败")
    void testVerifyChallenge_WrongOffset() {
        SliderChallengeDTO dto = SliderChallengeDTO.builder()
                .operation("LOGIN")
                .username("test_user")
                .build();
        SliderChallengeVO challenge = sliderCaptchaService.renderChallenge(dto, "127.0.0.1", "device_001");

        SliderVerifyDTO verifyDTO = SliderVerifyDTO.builder()
                .challengeId(challenge.getChallengeId())
                .offsetX(999) // 巨大偏差
                .durationMs(500)
                .events(List.of(new SliderTrackEvent(0, 0, 0), new SliderTrackEvent(999, 0, 500)))
                .username("test_user")
                .build();

        assertThrows(BusinessException.class, () ->
                sliderCaptchaService.verifyChallenge(verifyDTO, "127.0.0.1", "device_001")
        );
    }
}
