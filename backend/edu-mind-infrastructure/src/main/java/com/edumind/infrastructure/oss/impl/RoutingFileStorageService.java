package com.edumind.infrastructure.oss.impl;

import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.infrastructure.oss.StorageConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 动态路由文件存储门面服务 (支持管理后台热切换 local / minio / cos / oss)
 */
@Slf4j
@Primary
@Service
public class RoutingFileStorageService implements FileStorageService {

    @Value("${storage.type:local}")
    private String defaultType;

    @Value("${storage.local.dir:backend/data}")
    private String defaultLocalDir;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String defaultMinioEndpoint;

    @Value("${minio.accessKey:minioadmin}")
    private String defaultMinioAccessKey;

    @Value("${minio.secretKey:minioadmin}")
    private String defaultMinioSecretKey;

    @Value("${minio.bucketName:edumind}")
    private String defaultMinioBucket;

    private volatile FileStorageService delegate;
    private volatile StorageConfig currentConfig;

    @PostConstruct
    public void init() {
        StorageConfig initialConfig = StorageConfig.builder()
                .type(defaultType)
                .localPath(defaultLocalDir)
                .minioEndpoint(defaultMinioEndpoint)
                .minioAccessKey(defaultMinioAccessKey)
                .minioSecretKey(defaultMinioSecretKey)
                .minioBucket(defaultMinioBucket)
                .build();
        reload(initialConfig);
    }

    /**
     * 热重载当前激活的存储驱动
     */
    public synchronized void reload(StorageConfig config) {
        if (config == null) {
            config = StorageConfig.builder().type("local").build();
        }
        FileStorageService next = createDriver(config);
        FileStorageService prev = this.delegate;
        this.delegate = next;
        this.currentConfig = config;
        log.info("文件存储驱动已热切换成功: 引擎类型={}, 实例={}", next.getStorageType(), next.getClass().getSimpleName());
    }

    /**
     * 针对指定配置进行连通性测试 (不替换当前运行态驱动)
     */
    public boolean testConnection(StorageConfig config) {
        if (config == null) return false;
        try {
            FileStorageService probe = createDriver(config);
            return probe.testConnection();
        } catch (Exception e) {
            log.warn("存储驱动连通性测试异常: {}", e.getMessage());
            return false;
        }
    }

    public StorageConfig getCurrentConfig() {
        return currentConfig;
    }

    public FileStorageService createDriver(StorageConfig config) {
        String type = config != null && StringUtils.hasText(config.getType())
                ? config.getType().trim().toLowerCase() : "local";
        return switch (type) {
            case "minio" -> new MinioFileStorageService(
                    config.getMinioEndpoint(),
                    config.getMinioAccessKey(),
                    config.getMinioSecretKey(),
                    config.getMinioBucket()
            );
            case "cos" -> new CosFileStorageService(
                    config.getCosSecretId(),
                    config.getCosSecretKey(),
                    config.getCosRegion(),
                    config.getCosBucket(),
                    config.getCosDomain()
            );
            case "oss" -> new AliyunOssFileStorageService(
                    config.getOssEndpoint(),
                    config.getOssAccessKeyId(),
                    config.getOssAccessKeySecret(),
                    config.getOssBucket(),
                    config.getOssDomain()
            );
            default -> new LocalFileStorageService(config != null ? config.getLocalPath() : null);
        };
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        return delegate.uploadFile(bucketName, objectName, inputStream, contentType);
    }

    @Override
    public InputStream getFile(String bucketName, String objectName) {
        return delegate.getFile(bucketName, objectName);
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        delegate.deleteFile(bucketName, objectName);
    }

    @Override
    public String getStorageType() {
        return delegate != null ? delegate.getStorageType() : "local";
    }

    @Override
    public boolean testConnection() {
        return delegate != null && delegate.testConnection();
    }

    public String getResolvedStoragePath() {
        if (delegate instanceof LocalFileStorageService local) {
            return local.getStoragePath();
        }
        return currentConfig != null && StringUtils.hasText(currentConfig.getLocalPath())
                ? currentConfig.getLocalPath() : "backend/data";
    }
}
