package com.edumind.infrastructure.redis.gateway;

import lombok.Data;

import java.io.Serializable;

@Data
public class CircuitBreakerState implements Serializable {

    public enum Status {
        CLOSED, OPEN, HALF_OPEN
    }

    private Status status = Status.CLOSED;
    private int consecutiveFailures;
    private long openUntilMs;
    private long halfOpenProbeAtMs;
}
