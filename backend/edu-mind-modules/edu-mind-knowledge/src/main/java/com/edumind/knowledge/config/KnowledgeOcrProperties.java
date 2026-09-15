package com.edumind.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "knowledge.ocr")
public class KnowledgeOcrProperties {

    /** dev/test 默认 true；prod 必须 false */
    private Boolean mockEnabled = true;

    /** mock | paddle */
    private String engine = "mock";

    private Paddle paddle = new Paddle();

    @Data
    public static class Paddle {
        private String baseUrl = "";
        private int timeoutMs = 120000;
    }
}
