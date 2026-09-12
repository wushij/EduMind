package com.edumind.infrastructure.vector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edumind.milvus")
public class MilvusProperties {
    private boolean enabled = false;
    private String host = "localhost";
    private int port = 19530;
    private String collection = "edumind_chunks";
    private int dimension = 1536;
}
