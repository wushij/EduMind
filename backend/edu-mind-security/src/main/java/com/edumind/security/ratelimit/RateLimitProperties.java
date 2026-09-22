package com.edumind.security.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "edumind.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private List<Rule> rules = defaultRules();

    @Data
    public static class Rule {
        private String path;
        private String method = "*";
        private String dimension = "ip";
        private long limit = 60;
        private long windowSeconds = 60;
    }

    private static List<Rule> defaultRules() {
        List<Rule> rules = new ArrayList<>();
        rules.add(rule("POST", "/api/auth/login", "ip", 10));
        // 演示扫码登录是免密入口，同样按 IP 限流，防止被刷
        rules.add(rule("POST", "/api/auth/demo-scan-login", "ip", 10));
        rules.add(rule("POST", "/api/auth/register", "ip", 5));
        rules.add(rule("GET", "/api/auth/captcha", "ip", 30));
        rules.add(rule("POST", "/api/ai/chat/stream", "user", 20));
        rules.add(rule("POST", "/api/ai/questions/generate", "user", 10));
        rules.add(rule("POST", "/api/ai/exams/generate", "user", 5));
        return rules;
    }

    private static Rule rule(String method, String path, String dimension, long limit) {
        Rule rule = new Rule();
        rule.setMethod(method);
        rule.setPath(path);
        rule.setDimension(dimension);
        rule.setLimit(limit);
        rule.setWindowSeconds(60);
        return rule;
    }
}
