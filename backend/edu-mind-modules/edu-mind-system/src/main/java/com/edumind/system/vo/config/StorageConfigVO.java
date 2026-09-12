package com.edumind.system.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 存储引擎配置展示模型 VO (敏感密钥已脱敏)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageConfigVO implements Serializable {

    private String type;
    private String localPath;

    // === MinIO ===
    private String minioEndpoint;
    private String minioAccessKey;
    private String minioBucket;
    private boolean minioSecretKeyConfigured;

    // === 腾讯云 COS ===
    private String cosSecretId;
    private String cosRegion;
    private String cosBucket;
    private String cosDomain;
    private boolean cosSecretKeyConfigured;

    // === 阿里云 OSS ===
    private String ossEndpoint;
    private String ossAccessKeyId;
    private String ossBucket;
    private String ossDomain;
    private boolean ossAccessKeySecretConfigured;

    /** 当前运行态信息 */
    private String activeType;
    private String activeBucket;
    private boolean healthy;
}
