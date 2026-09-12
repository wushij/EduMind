package com.edumind.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "edumind.security")
public class SecurityProperties {
    private boolean smEnabled = false;
    private String hmacSecret = "EduMind_Platform_SecretKey_2026";
    private long timestampSkewMs = 5 * 60 * 1000L;
    private List<String> sensitivePaths = new ArrayList<>(List.of("/api/analytics", "/api/ai/agent"));
}
