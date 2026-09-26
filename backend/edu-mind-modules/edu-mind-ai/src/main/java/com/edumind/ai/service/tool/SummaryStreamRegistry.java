package com.edumind.ai.service.tool;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 总结流式生成注册表（与 {@code ChatStreamRegistry} 同构）。
 *
 * <p>总结与聊天是两条独立的流式链路，各自的取消信号生命周期不同：
 * 用户可以在课程问答仍在进行时单独中止一次总结推演，因此这里单独维护，
 * 不与聊天流共用一个注册表，避免误取消对方的生成。</p>
 */
@Component
public class SummaryStreamRegistry {

    private final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    public String register() {
        String streamId = UUID.randomUUID().toString();
        cancelFlags.put(streamId, new AtomicBoolean(false));
        return streamId;
    }

    public void cancel(String streamId) {
        AtomicBoolean flag = cancelFlags.get(streamId);
        if (flag != null) {
            flag.set(true);
        }
    }

    public boolean isCancelled(String streamId) {
        AtomicBoolean flag = cancelFlags.get(streamId);
        return flag != null && flag.get();
    }

    public void remove(String streamId) {
        cancelFlags.remove(streamId);
    }
}
