package com.edumind.security.captcha;

import cn.hutool.core.util.IdUtil;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.RedisService;
import com.edumind.infrastructure.redis.RedisSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 滑块验证码业务服务 (自研高精拼图生成 + 多维人机轨迹风控打分，对齐 Code Compass)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SliderCaptchaService {

    public static final int SLIDER_WIDTH = 320;
    public static final int SLIDER_HEIGHT = 160;
    private static final String CHALLENGE_PREFIX = "captcha:slider:challenge:";
    private static final String TOKEN_PREFIX = "captcha:slider:token:";
    private static final long CHALLENGE_TTL_SECONDS = 180;
    private static final long TOKEN_TTL_SECONDS = 120;
    private static final int DEFAULT_TOLERANCE = 6;
    private static final int BEHAVIOR_PASS_SCORE = 70;
    private static final int MAX_FAIL_COUNT = 3;

    private final RedisService redisService;
    private final RedisSupport redisSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();

    private record LocalCacheEntry(String value, long expireAt) {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChallengeStore implements Serializable {
        private String challengeId;
        private int targetX;
        private int targetY;
        private int tolerance;
        private int pieceSize;
        private int width;
        private String operation;
        private String username;
        private String deviceId;
        private String clientIp;
        private int failCount;
        private long createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenStore implements Serializable {
        private String challengeId;
        private String operation;
        private String username;
        private String deviceId;
        private String clientIp;
        private long createdAt;
    }

    /**
     * 生成滑块拼图挑战
     */
    public SliderChallengeVO renderChallenge(SliderChallengeDTO req, String clientIp, String deviceId) {
        String operation = (req != null && StringUtils.hasText(req.getOperation()))
                ? req.getOperation().trim()
                : "LOGIN";
        String username = (req != null && StringUtils.hasText(req.getUsername()))
                ? req.getUsername().trim()
                : "";

        BufferedImage sourceBg = loadBackgroundCover(SLIDER_WIDTH, SLIDER_HEIGHT);

        int pieceSize = 44 + ThreadLocalRandom.current().nextInt(9); // 44 ~ 52
        int minX = pieceSize;
        int maxX = SLIDER_WIDTH - pieceSize * 2;
        if (maxX < minX) {
            maxX = minX;
        }
        int targetX = minX + ThreadLocalRandom.current().nextInt(maxX - minX + 1);

        int minY = 20;
        int maxY = SLIDER_HEIGHT - pieceSize - 12;
        if (maxY < minY) {
            maxY = minY;
        }
        int targetY = minY + ThreadLocalRandom.current().nextInt(maxY - minY + 1);

        BufferedImage bgImage = new BufferedImage(SLIDER_WIDTH, SLIDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D gBg = bgImage.createGraphics();
        gBg.drawImage(sourceBg, 0, 0, null);
        gBg.dispose();

        BufferedImage pieceImage = new BufferedImage(pieceSize, pieceSize, BufferedImage.TYPE_INT_ARGB);
        drawPiece(pieceImage, sourceBg, targetX, targetY, pieceSize);
        cutHole(bgImage, targetX, targetY, pieceSize);

        String bgBase64 = encodeJpegBase64(bgImage);
        String pieceBase64 = encodePngBase64(pieceImage);

        String challengeId = "sc_" + IdUtil.simpleUUID();
        long expireAt = System.currentTimeMillis() + CHALLENGE_TTL_SECONDS * 1000;

        ChallengeStore store = ChallengeStore.builder()
                .challengeId(challengeId)
                .targetX(targetX)
                .targetY(targetY)
                .tolerance(DEFAULT_TOLERANCE)
                .pieceSize(pieceSize)
                .width(SLIDER_WIDTH)
                .operation(operation)
                .username(username)
                .deviceId(deviceId != null ? deviceId.trim() : "")
                .clientIp(clientIp != null ? clientIp.trim() : "")
                .failCount(0)
                .createdAt(System.currentTimeMillis())
                .build();

        saveCacheJson(CHALLENGE_PREFIX + challengeId, store, CHALLENGE_TTL_SECONDS);

        return SliderChallengeVO.builder()
                .challengeId(challengeId)
                .background(bgBase64)
                .piece(pieceBase64)
                .width(SLIDER_WIDTH)
                .height(SLIDER_HEIGHT)
                .pieceSize(pieceSize)
                .pieceY(targetY)
                .expireAt(expireAt)
                .build();
    }

    /**
     * 校验滑块拖拽轨迹并签发凭据 Token
     */
    public SliderVerifyVO verifyChallenge(SliderVerifyDTO req, String clientIp, String deviceId) {
        if (req == null || !StringUtils.hasText(req.getChallengeId())) {
            throw new BusinessException("验证码已过期，请刷新");
        }

        String key = CHALLENGE_PREFIX + req.getChallengeId().trim();
        ChallengeStore store = getCacheJson(key, ChallengeStore.class);
        if (store == null) {
            throw new BusinessException("验证码已过期，请刷新");
        }

        int score = scoreSliderBehavior(req, store, deviceId);
        int diff = Math.abs(req.getOffsetX() - store.getTargetX());
        boolean positionOk = diff <= store.getTolerance();

        if (!positionOk || score < BEHAVIOR_PASS_SCORE) {
            store.setFailCount(store.getFailCount() + 1);
            if (store.getFailCount() >= MAX_FAIL_COUNT) {
                deleteCacheKey(key);
            } else {
                saveCacheJson(key, store, CHALLENGE_TTL_SECONDS);
            }
            if (score < 40) {
                throw new BusinessException("行为异常，请重新验证");
            }
            throw new BusinessException("验证失败，请重试");
        }

        deleteCacheKey(key);

        String captchaToken = "ct_slider_" + IdUtil.simpleUUID();
        long expireAt = System.currentTimeMillis() + TOKEN_TTL_SECONDS * 1000;

        TokenStore tokenStore = TokenStore.builder()
                .challengeId(store.getChallengeId())
                .operation(store.getOperation())
                .username(StringUtils.hasText(req.getUsername()) ? req.getUsername().trim() : store.getUsername())
                .deviceId(deviceId != null ? deviceId.trim() : store.getDeviceId())
                .clientIp(clientIp != null ? clientIp.trim() : store.getClientIp())
                .createdAt(System.currentTimeMillis())
                .build();

        saveCacheJson(TOKEN_PREFIX + captchaToken, tokenStore, TOKEN_TTL_SECONDS);

        return SliderVerifyVO.builder()
                .captchaToken(captchaToken)
                .expireAt(expireAt)
                .build();
    }

    /**
     * 业务端消费滑块 Token (一次性消费，防重放攻击)
     */
    public void consumeToken(String token, String username, String operation, String deviceId) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("请完成安全验证");
        }

        String key = TOKEN_PREFIX + token.trim();
        TokenStore store = getCacheJson(key, TokenStore.class);
        deleteCacheKey(key);

        if (store == null) {
            throw new BusinessException("安全验证已失效，请重新验证");
        }
        if (StringUtils.hasText(username) && StringUtils.hasText(store.getUsername())
                && !store.getUsername().equalsIgnoreCase(username.trim())) {
            throw new BusinessException("安全验证主体不符，请重新验证");
        }
        if (StringUtils.hasText(operation) && StringUtils.hasText(store.getOperation())
                && !store.getOperation().equalsIgnoreCase(operation.trim())) {
            throw new BusinessException("安全验证类型不符，请重新验证");
        }
    }

    /**
     * 获取系统当前验证码策略配置
     */
    public CaptchaPolicyVO getCaptchaPolicy() {
        boolean captchaEnabled = true;
        String captchaType = "slider";
        boolean smsLoginSliderCaptchaEnabled = false;
        boolean emailLoginSliderCaptchaEnabled = false;

        try {
            String configJson = null;
            if (redisSupport.useRedisOrFallback()) {
                configJson = redisService.get("sys:config:sys.login.config");
            }
            if (configJson != null) {
                JsonNode root = objectMapper.readTree(configJson);
                if (root.has("captchaEnabled")) {
                    captchaEnabled = root.get("captchaEnabled").asBoolean(true);
                }
                if (root.has("captchaType")) {
                    captchaType = root.get("captchaType").asText("slider");
                }
                if (root.has("smsLoginSliderCaptchaEnabled")) {
                    smsLoginSliderCaptchaEnabled = root.get("smsLoginSliderCaptchaEnabled").asBoolean(false);
                }
                if (root.has("emailLoginSliderCaptchaEnabled")) {
                    emailLoginSliderCaptchaEnabled = root.get("emailLoginSliderCaptchaEnabled").asBoolean(false);
                }
            }
        } catch (Exception ex) {
            log.warn("读取验证码策略配置异常，采用默认滑块配置: {}", ex.getMessage());
        }

        return CaptchaPolicyVO.builder()
                .captchaEnabled(captchaEnabled)
                .captchaType(captchaType)
                .smsLoginSliderCaptchaEnabled(smsLoginSliderCaptchaEnabled)
                .emailLoginSliderCaptchaEnabled(emailLoginSliderCaptchaEnabled)
                .build();
    }

    /**
     * 人机行为轨迹风控评分算法 (对齐 Code Compass scoreSliderBehavior)
     */
    private int scoreSliderBehavior(SliderVerifyDTO req, ChallengeStore store, String deviceId) {
        int score = 0;

        int diff = Math.abs(req.getOffsetX() - store.getTargetX());
        if (diff <= 3) {
            score += 30;
        } else if (diff <= 5) {
            score += 25;
        } else if (diff <= 8) {
            score += 15;
        }

        int dur = req.getDurationMs();
        if (dur >= 400 && dur <= 12000) {
            score += 15;
        } else if ((dur >= 200 && dur < 400) || (dur > 12000 && dur <= 15000)) {
            score += 8;
        }

        List<SliderTrackEvent> events = req.getEvents();
        int n = (events != null) ? events.size() : 0;
        if (n >= 10) {
            score += 15;
        } else if (n >= 6) {
            score += 10;
        } else if (n >= 3) {
            score += 5;
        }

        if (events != null && !events.isEmpty()) {
            score += smoothnessScore(events);
            score += velocityChangeScore(events);
            score += yJitterScore(events);
        }

        if (StringUtils.hasText(store.getDeviceId()) && StringUtils.hasText(deviceId)) {
            if (store.getDeviceId().equalsIgnoreCase(deviceId.trim())) {
                score += 5;
            }
        } else {
            score += 5;
        }

        return score;
    }

    private int smoothnessScore(List<SliderTrackEvent> events) {
        if (events.size() < 3) return 0;
        List<Double> steps = new ArrayList<>();
        for (int i = 1; i < events.size(); i++) {
            double dx = events.get(i).getX() - events.get(i - 1).getX();
            double dt = events.get(i).getT() - events.get(i - 1).getT();
            if (dt <= 0) continue;
            steps.add(Math.abs(dx / dt));
        }
        if (steps.isEmpty()) return 0;
        double sum = 0.0;
        double max = 0.0;
        for (double s : steps) {
            sum += s;
            if (s > max) max = s;
        }
        double avg = sum / steps.size();
        if (avg <= 0) return 0;
        double ratio = max / avg;
        if (ratio >= 1.2 && ratio <= 6.0) {
            return 15;
        }
        if (ratio > 6.0 && ratio <= 12.0) {
            return 8;
        }
        return 5;
    }

    private int velocityChangeScore(List<SliderTrackEvent> events) {
        if (events.size() < 4) return 0;
        int changes = 0;
        double prev = 0.0;
        for (int i = 1; i < events.size(); i++) {
            double dt = events.get(i).getT() - events.get(i - 1).getT();
            if (dt <= 0) continue;
            double v = (events.get(i).getX() - events.get(i - 1).getX()) / dt;
            if (i > 1 && Math.abs(v - prev) > 0.05) {
                changes++;
            }
            prev = v;
        }
        if (changes >= 2) return 10;
        if (changes >= 1) return 6;
        return 0;
    }

    private int yJitterScore(List<SliderTrackEvent> events) {
        if (events.size() < 2) return 0;
        for (SliderTrackEvent e : events) {
            if ((e.getY() >= 1 && e.getY() <= 4) || (e.getY() <= -1 && e.getY() >= -4)) {
                return 5;
            }
        }
        return 0;
    }

    /**
     * 经典拼图块形状掩码：圆角矩形主体 + 右侧半圆凸起
     */
    private boolean pieceMask(int px, int py, int size) {
        double cx = size / 2.0;
        double cy = size / 2.0;
        double r = size / 2.0 - 3.0;
        double dx = px - cx;
        double dy = py - cy;
        boolean inRect = Math.abs(dx) <= r - 2 && Math.abs(dy) <= r - 2;

        double bumpCX = size - 5.0;
        double bumpCY = cy;
        double bumpR = 7.0;
        boolean inBump = (px - bumpCX) * (px - bumpCX) + (py - bumpCY) * (py - bumpCY) <= bumpR * bumpR;

        return inRect || inBump;
    }

    /**
     * 抠出拼图滑块
     */
    private void drawPiece(BufferedImage dst, BufferedImage src, int targetX, int targetY, int size) {
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                if (!pieceMask(px, py, size)) {
                    dst.setRGB(px, py, 0x00000000);
                    continue;
                }
                int rgb = src.getRGB(targetX + px, targetY + py);
                dst.setRGB(px, py, rgb);
            }
        }
    }

    /**
     * 在底图上压暗抠出缺口 (42% 亮度保留背景纹理)
     */
    private void cutHole(BufferedImage bg, int targetX, int targetY, int size) {
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                if (!pieceMask(px, py, size)) {
                    continue;
                }
                int rgb = bg.getRGB(targetX + px, targetY + py);
                int r = ((rgb >> 16) & 0xFF) * 42 / 100;
                int g = ((rgb >> 8) & 0xFF) * 42 / 100;
                int b = (rgb & 0xFF) * 42 / 100;
                int darkened = (r << 16) | (g << 8) | b;
                bg.setRGB(targetX + px, targetY + py, darkened);
            }
        }
    }

    /**
     * 读取背景图并做 Cover 裁剪居中缩放到 targetW x targetH
     */
    private BufferedImage loadBackgroundCover(int targetW, int targetH) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:captcha-bg/*.jpg");
            if (resources.length > 0) {
                Resource chosen = resources[ThreadLocalRandom.current().nextInt(resources.length)];
                try (InputStream in = chosen.getInputStream()) {
                    BufferedImage orig = ImageIO.read(in);
                    if (orig != null) {
                        return resizeCover(orig, targetW, targetH);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("加载滑块背景图资源失败，切换为程序化渐变底图: {}", ex.getMessage());
        }
        return generateProceduralBackground(targetW, targetH);
    }

    private BufferedImage resizeCover(BufferedImage src, int targetW, int targetH) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        double scale = Math.max((double) targetW / sw, (double) targetH / sh);
        int nw = (int) Math.round(sw * scale);
        int nh = (int) Math.round(sh * scale);

        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = scaled.createGraphics();
        g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g1.drawImage(src, 0, 0, nw, nh, null);
        g1.dispose();

        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        int ox = (nw - targetW) / 2;
        int oy = (nh - targetH) / 2;
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(scaled, -ox, -oy, null);
        g2.dispose();
        return out;
    }

    /**
     * 程序化优雅渐变背景生成 (素材加载失败时的降级方案)
     */
    private BufferedImage generateProceduralBackground(int w, int h) {
        BufferedImage m = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = m.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color c1 = new Color(40 + ThreadLocalRandom.current().nextInt(80),
                80 + ThreadLocalRandom.current().nextInt(100),
                140 + ThreadLocalRandom.current().nextInt(80));
        Color c2 = new Color(110 + ThreadLocalRandom.current().nextInt(90),
                140 + ThreadLocalRandom.current().nextInt(80),
                180 + ThreadLocalRandom.current().nextInt(70));

        for (int y = 0; y < h; y++) {
            float t = (float) y / h;
            int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * t);
            int gr = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
            int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
            g.setColor(new Color(r, gr, b));
            g.drawLine(0, y, w, y);
        }

        // 绘制柔和圆形噪点
        for (int i = 0; i < 40; i++) {
            int cx = ThreadLocalRandom.current().nextInt(w);
            int cy = ThreadLocalRandom.current().nextInt(h);
            int radius = 10 + ThreadLocalRandom.current().nextInt(30);
            g.setColor(new Color(255, 255, 255, 18));
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
        g.dispose();
        return m;
    }

    private String encodeJpegBase64(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException("验证码生成失败: " + ex.getMessage());
        }
    }

    private String encodePngBase64(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException("验证码生成失败: " + ex.getMessage());
        }
    }

    private void saveCacheJson(String key, Object obj, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            if (redisSupport.useRedisOrFallback()) {
                try {
                    redisService.set(key, json, ttlSeconds);
                    return;
                } catch (Exception ex) {
                    if (redisSupport.requireRedis()) {
                        throw new BusinessException("验证码服务不可用");
                    }
                    log.warn("Redis 写入失败，降级本地内存: {}", ex.getMessage());
                }
            }
            localCache.put(key, new LocalCacheEntry(json, System.currentTimeMillis() + ttlSeconds * 1000));
        } catch (Exception e) {
            log.error("缓存写入异常: {}", e.getMessage());
        }
    }

    private <T> T getCacheJson(String key, Class<T> clazz) {
        try {
            String json = null;
            if (redisSupport.useRedisOrFallback()) {
                try {
                    json = redisService.get(key);
                } catch (Exception ex) {
                    if (redisSupport.requireRedis()) {
                        throw new BusinessException("验证码服务不可用");
                    }
                }
            }
            if (json == null) {
                LocalCacheEntry entry = localCache.get(key);
                if (entry != null && System.currentTimeMillis() <= entry.expireAt()) {
                    json = entry.value();
                }
            }
            if (json != null) {
                return objectMapper.readValue(json, clazz);
            }
        } catch (Exception e) {
            log.error("缓存读取反序列化异常: {}", e.getMessage());
        }
        return null;
    }

    private void deleteCacheKey(String key) {
        localCache.remove(key);
        if (redisSupport.useRedisOrFallback()) {
            try {
                redisService.delete(key);
            } catch (Exception ignored) {
            }
        }
    }
}