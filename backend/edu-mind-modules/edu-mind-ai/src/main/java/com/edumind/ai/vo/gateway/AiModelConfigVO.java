package com.edumind.ai.vo.gateway;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiModelConfigVO {
    private Long id;
    /** 配置唯一标识 */
    private String name;
    private String provider;
    private String configType;
    private String modelName;
    private String baseUrl;
    /** 仅写入，不回传 */
    private String apiKey;
    private Boolean hasApiKey;
    /** KMS 密钥版本号 */
    private Integer keyVersion;
    private BigDecimal temperature;
    private String reasoningEffort;
    private Integer dimension;
    /** enabled | disabled */
    private String status;
    private Boolean isDefault;
    private Integer sortOrder;

    /** 兼容旧 Gateway 字段 */
    private String modelKey;
    private Boolean enabled;
    private Integer priority;
    private String fallbackModelKey;
    private Integer maxTokens;
}
