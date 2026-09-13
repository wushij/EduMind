package com.edumind.infrastructure.oss.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.common.utils.TenantObjectKeyBuilder;
import com.edumind.infrastructure.oss.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 腾讯云 COS 对象存储驱动实现 (基于 S3 兼容协议通道)
 */
@Slf4j
public class CosFileStorageService implements FileStorageService {

    private final MinioClient cosClient;
    private final String defaultBucket;
    private final String region;
    private final String cdnDomain;

    public CosFileStorageService(String secretId, String secretKey, String region, String bucket, String cdnDomain) {
        if (!StringUtils.hasText(secretId) || !StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("腾讯云 COS 密钥未配置 (SecretId / SecretKey)");
        }
        if (!StringUtils.hasText(region)) {
            throw new IllegalArgumentException("腾讯云 COS 区域未配置 (如 ap-guangzhou)");
        }
        this.defaultBucket = StringUtils.hasText(bucket) ? bucket : "edumind";
        this.region = region.trim();
        this.cdnDomain = cdnDomain != null ? cdnDomain.trim() : null;

        String endpoint = String.format("https://cos.%s.myqcloud.com", this.region);
        this.cosClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(secretId.trim(), secretKey.trim())
                .region(this.region)
                .build();
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName : defaultBucket;
        try {
            cosClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, -1, 10485760)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            return buildUrl(bucket, objectName);
        } catch (Exception e) {
            log.error("腾讯云 COS 上传失败: {}", e.getMessage(), e);
            throw new IllegalStateException("腾讯云 COS 上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getFile(String bucketName, String objectName) {
        Long currentTenantId = TenantContext.getTenantId();
        if (!TenantObjectKeyBuilder.validateTenantOwnership(currentTenantId, objectName)) {
            log.warn("拒绝跨租户读取腾讯云COS存储文件: tenantId={}, objectName={}", currentTenantId, objectName);
            return null;
        }
        String bucket = StringUtils.hasText(bucketName) ? bucketName : defaultBucket;
        try {
            return cosClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectName).build()
            );
        } catch (Exception e) {
            log.warn("腾讯云 COS 获取文件失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        Long currentTenantId = TenantContext.getTenantId();
        if (!TenantObjectKeyBuilder.validateTenantOwnership(currentTenantId, objectName)) {
            log.warn("拒绝跨租户删除腾讯云COS存储文件: tenantId={}, objectName={}", currentTenantId, objectName);
            return;
        }
        String bucket = StringUtils.hasText(bucketName) ? bucketName : defaultBucket;
        try {
            cosClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(objectName).build()
            );
        } catch (Exception e) {
            log.warn("腾讯云 COS 删除文件异常: {}", e.getMessage());
        }
    }

    @Override
    public String getStorageType() {
        return "cos";
    }

    @Override
    public boolean testConnection() {
        try {
            return cosClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucket).build());
        } catch (Exception e) {
            log.warn("腾讯云 COS 连通性测试失败: {}", e.getMessage());
            return false;
        }
    }

    private String buildUrl(String bucket, String objectName) {
        if (StringUtils.hasText(cdnDomain)) {
            String domain = cdnDomain.endsWith("/") ? cdnDomain.substring(0, cdnDomain.length() - 1) : cdnDomain;
            return domain + "/" + objectName;
        }
        return String.format("https://%s.cos.%s.myqcloud.com/%s", bucket, region, objectName);
    }
}
