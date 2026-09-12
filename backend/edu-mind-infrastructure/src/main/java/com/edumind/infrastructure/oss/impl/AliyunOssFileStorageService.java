package com.edumind.infrastructure.oss.impl;

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
 * 阿里云 OSS 对象存储驱动实现 (基于 S3 兼容协议通道)
 */
@Slf4j
public class AliyunOssFileStorageService implements FileStorageService {

    private final MinioClient ossClient;
    private final String defaultBucket;
    private final String endpoint;
    private final String customDomain;

    public AliyunOssFileStorageService(String endpoint, String accessKeyId, String accessKeySecret, String bucket, String customDomain) {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("阿里云 OSS Endpoint 未配置 (如 https://oss-cn-hangzhou.aliyuncs.com)");
        }
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            throw new IllegalArgumentException("阿里云 OSS 访问密钥未配置 (AccessKeyId / AccessKeySecret)");
        }
        this.defaultBucket = StringUtils.hasText(bucket) ? bucket : "edumind";
        this.endpoint = endpoint.trim();
        this.customDomain = customDomain != null ? customDomain.trim() : null;

        this.ossClient = MinioClient.builder()
                .endpoint(this.endpoint)
                .credentials(accessKeyId.trim(), accessKeySecret.trim())
                .build();
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName : defaultBucket;
        try {
            ossClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, -1, 10485760)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            return buildUrl(bucket, objectName);
        } catch (Exception e) {
            log.error("阿里云 OSS 上传失败: {}", e.getMessage(), e);
            throw new IllegalStateException("阿里云 OSS 上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getFile(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName : defaultBucket;
        try {
            return ossClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectName).build()
            );
        } catch (Exception e) {
            log.warn("阿里云 OSS 获取文件失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName : defaultBucket;
        try {
            ossClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(objectName).build()
            );
        } catch (Exception e) {
            log.warn("阿里云 OSS 删除文件异常: {}", e.getMessage());
        }
    }

    @Override
    public String getStorageType() {
        return "oss";
    }

    @Override
    public boolean testConnection() {
        try {
            return ossClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucket).build());
        } catch (Exception e) {
            log.warn("阿里云 OSS 连通性测试失败: {}", e.getMessage());
            return false;
        }
    }

    private String buildUrl(String bucket, String objectName) {
        if (StringUtils.hasText(customDomain)) {
            String domain = customDomain.endsWith("/") ? customDomain.substring(0, customDomain.length() - 1) : customDomain;
            return domain + "/" + objectName;
        }
        String cleanEndpoint = endpoint.replaceFirst("^https?://", "");
        return String.format("https://%s.%s/%s", bucket, cleanEndpoint, objectName);
    }
}
