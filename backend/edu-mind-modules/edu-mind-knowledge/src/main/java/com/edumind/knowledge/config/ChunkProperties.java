package com.edumind.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edumind.chunk")
public class ChunkProperties {
    private int size = 800;
    private int overlap = 128;
    private int charsPerPage = 3000;
}
