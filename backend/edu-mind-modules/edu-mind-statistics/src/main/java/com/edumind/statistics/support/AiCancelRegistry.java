package com.edumind.statistics.support;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错题本 AI 调用的「中止」登记表。
 *
 * <p>背景：浏览器 AbortController 只能取消前端等待，关掉 HTTP 连接并不会中断服务端线程，
 * 那次已经发出的模型调用仍会跑完（费用也已在上游产生）。但服务端可以做到两件事止损：</p>
 * <ol>
 *   <li>模型返回后<b>丢弃结果、不再落库</b>，避免用户"点了中止却还是冒出新结论"；</li>
 *   <li>变式题不再写入题库，避免中止的生成污染题目数据。</li>
 * </ol>
 *
 * <p>登记项带 TTL，避免用户中止后不再操作导致标记长期残留、影响后续正常生成。</p>
 */
@Component
public class AiCancelRegistry {

    /** 中止标记有效期：超过则视为过期自动清理 */
    private static final long TTL_MS = 5 * 60 * 1000L;

    private final Map<String, Long> cancelledAt = new ConcurrentHashMap<>();

    public void markCancelled(String key) {
        if (key != null) {
            cancelledAt.put(key, System.currentTimeMillis());
        }
    }

    public boolean isCancelled(String key) {
        if (key == null) {
            return false;
        }
        Long markedAt = cancelledAt.get(key);
        if (markedAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - markedAt > TTL_MS) {
            cancelledAt.remove(key);
            return false;
        }
        return true;
    }

    public void clear(String key) {
        if (key != null) {
            cancelledAt.remove(key);
        }
    }
}
