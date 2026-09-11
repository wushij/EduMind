package com.edumind.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStartupValidator implements ApplicationRunner {

    private final RedisSupport redisSupport;

    @Override
    public void run(ApplicationArguments args) {
        if (redisSupport.requireRedis() && !redisSupport.useRedisOrFallback()) {
            throw new IllegalStateException("生产环境启动失败：Redis 不可用，请先启动 Redis 服务");
        }
        if (redisSupport.useRedisOrFallback()) {
            log.info("Redis 连接检查通过");
        } else {
            log.warn("当前运行在 dev 模式且 Redis 不可用，验证码/防重放将使用本地内存降级");
        }
    }
}
