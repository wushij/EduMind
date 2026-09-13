package com.edumind.infrastructure.redis.gateway;

import com.edumind.common.exception.BusinessException;
import com.edumind.common.api.ResultCode;
import com.edumind.infrastructure.redis.RateLimitService;
import com.edumind.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Gateway 生产级韧性存储：Redis 分布式限流 + 熔断状态机 + 指标计数。
 */
@Service
@RequiredArgsConstructor
public class GatewayResilienceStore {

    private static final String RATE_PREFIX = "edumind:gateway:rate:";
    private static final String CIRCUIT_PREFIX = "edumind:gateway:circuit:";
    private static final String METRIC_PREFIX = "edumind:gateway:metric:";
    private static final long RATE_WINDOW_SECONDS = 60;
    private static final long CIRCUIT_OPEN_MS = 60_000;
    private static final int FAILURE_THRESHOLD = 3;

    private final RateLimitService rateLimitService;
    private final RedisService redisService;

    private final AtomicLong localFallbackCount = new AtomicLong();
    private final AtomicLong localFailedCount = new AtomicLong();
    private final AtomicLong localRetryCount = new AtomicLong();
    private final AtomicLong localRateLimitedCount = new AtomicLong();
    private final AtomicLong localCircuitOpenCount = new AtomicLong();

    public void checkRateLimit(String scene, int maxPerMinute) {
        String key = RATE_PREFIX + scene;
        if (!rateLimitService.allow(key, maxPerMinute, RATE_WINDOW_SECONDS)) {
            incrementMetric("rateLimitedCount");
            localRateLimitedCount.incrementAndGet();
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }
    }

    public boolean isCircuitOpen(String modelKey) {
        CircuitBreakerState state = getCircuitState(modelKey);
        long now = System.currentTimeMillis();
        if (state.getStatus() == CircuitBreakerState.Status.OPEN) {
            if (now >= state.getOpenUntilMs()) {
                state.setStatus(CircuitBreakerState.Status.HALF_OPEN);
                state.setHalfOpenProbeAtMs(now);
                saveCircuitState(modelKey, state);
                return false;
            }
            return true;
        }
        return false;
    }

    public void recordSuccess(String modelKey) {
        CircuitBreakerState state = getCircuitState(modelKey);
        state.setStatus(CircuitBreakerState.Status.CLOSED);
        state.setConsecutiveFailures(0);
        state.setOpenUntilMs(0);
        saveCircuitState(modelKey, state);
    }

    public void recordFailure(String modelKey) {
        CircuitBreakerState state = getCircuitState(modelKey);
        if (state.getStatus() == CircuitBreakerState.Status.HALF_OPEN) {
            openCircuit(modelKey, state);
            return;
        }
        state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
        if (state.getConsecutiveFailures() >= FAILURE_THRESHOLD) {
            openCircuit(modelKey, state);
        } else {
            saveCircuitState(modelKey, state);
        }
    }

    public void forceOpenCircuit(String modelKey) {
        CircuitBreakerState state = getCircuitState(modelKey);
        openCircuit(modelKey, state);
    }

    public void resetCircuit(String modelKey) {
        CircuitBreakerState state = new CircuitBreakerState();
        saveCircuitState(modelKey, state);
    }

    public void recordFallback() {
        incrementMetric("fallbackCount");
        localFallbackCount.incrementAndGet();
    }

    public void recordFailed() {
        incrementMetric("failedCount");
        localFailedCount.incrementAndGet();
    }

    public void recordRetry() {
        incrementMetric("retryCount");
        localRetryCount.incrementAndGet();
    }

    public void recordCircuitOpen() {
        incrementMetric("circuitOpenCount");
        localCircuitOpenCount.incrementAndGet();
    }

    public long getMetric(String name) {
        String val = redisService.get(METRIC_PREFIX + name);
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return switch (name) {
            case "fallbackCount" -> localFallbackCount.get();
            case "failedCount" -> localFailedCount.get();
            case "retryCount" -> localRetryCount.get();
            case "rateLimitedCount" -> localRateLimitedCount.get();
            case "circuitOpenCount" -> localCircuitOpenCount.get();
            default -> 0L;
        };
    }

    private void openCircuit(String modelKey, CircuitBreakerState state) {
        state.setStatus(CircuitBreakerState.Status.OPEN);
        state.setOpenUntilMs(System.currentTimeMillis() + CIRCUIT_OPEN_MS);
        saveCircuitState(modelKey, state);
        recordCircuitOpen();
    }

    private CircuitBreakerState getCircuitState(String modelKey) {
        CircuitBreakerState state = redisService.getObject(CIRCUIT_PREFIX + modelKey, CircuitBreakerState.class);
        return state != null ? state : new CircuitBreakerState();
    }

    private void saveCircuitState(String modelKey, CircuitBreakerState state) {
        redisService.setObject(CIRCUIT_PREFIX + modelKey, state, 3600);
    }

    private void incrementMetric(String name) {
        redisService.increment(METRIC_PREFIX + name, 86400);
    }
}
