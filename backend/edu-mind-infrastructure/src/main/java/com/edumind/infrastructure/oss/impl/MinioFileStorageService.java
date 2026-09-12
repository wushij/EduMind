package com.edumind.infrastructure.oss.impl;

import com.edumind.infrastructure.oss.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MinioFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorageService.class);

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.accessKey:minioadmin}")
    private String accessKey;

    @Value("${minio.secretKey:minioadmin}")
    private String secretKey;

    @Value("${minio.bucketName:edumind}")
    private String defaultBucketName;

    private volatile MinioClient minioClient;
    private volatile boolean minioAvailable;

    @Value("${storage.local.dir:}")
    private String customStorageDir;

    private volatile Path localStorageDir;
    private final Map<String, Path> localFallbackFiles = new ConcurrentHashMap<>();

    public MinioFileStorageService() {
    }

    public MinioFileStorageService(String endpoint, String accessKey, String secretKey, String defaultBucketName) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.defaultBucketName = defaultBucketName != null && !defaultBucketName.isBlank() ? defaultBucketName : "edumind";
    }

    @Override
    public String getStorageType() {
        return "minio";
    }

    @Override
    public boolean testConnection() {
        try {
            if (!useMinio()) {
                return false;
            }
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucketName).build());
        } catch (Exception e) {
            log.warn("MinIO 连通性测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        String bucket = bucketName != null ? bucketName : defaultBucketName;
        if (useMinio()) {
            try {
                ensureBucket(bucket);
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(inputStream, -1, 10485760)
                                .contentType(contentType)
                                .build()
                );
                return buildMinioUrl(bucket, objectName);
            } catch (Exception ex) {
                log.warn("MinIO upload failed, fallback to local storage: {}", ex.getMessage());
                disableMinio();
            }
        }
        return storeLocally(bucket, objectName, inputStream);
    }

    @Override
    public InputStream getFile(String bucketName, String objectName) {
        String bucket = bucketName != null ? bucketName : defaultBucketName;
        if (useMinio()) {
            try {
                return minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucket).object(objectName).build()
                );
            } catch (Exception ex) {
                log.warn("MinIO get failed, fallback to local storage: {}", ex.getMessage());
                disableMinio();
            }
        }
        Path localPath = resolveLocalPath(bucket, objectName);
        if (!Files.exists(localPath)) {
            return null;
        }
        try {
            return Files.newInputStream(localPath);
        } catch (IOException ex) {
            log.warn("Local file read failed: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        String bucket = bucketName != null ? bucketName : defaultBucketName;
        if (useMinio()) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder().bucket(bucket).object(objectName).build()
                );
                return;
            } catch (Exception ex) {
                log.warn("MinIO delete failed, fallback to local storage: {}", ex.getMessage());
                disableMinio();
            }
        }
        Path localPath = resolveLocalPath(bucket, objectName);
        localFallbackFiles.remove(buildLocalKey(bucket, objectName));
        try {
            Files.deleteIfExists(localPath);
        } catch (IOException ex) {
            log.warn("Local file delete failed: {}", ex.getMessage());
        }
    }

    private boolean useMinio() {
        if (!minioAvailable && minioClient == null) {
            initMinioClient();
        }
        return minioAvailable && minioClient != null;
    }

    private void initMinioClient() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            minioClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucketName).build());
            minioAvailable = true;
        } catch (Exception ex) {
            log.warn("MinIO unavailable, using local fallback storage: {}", ex.getMessage());
            minioAvailable = false;
            minioClient = null;
        }
    }

    private void disableMinio() {
        minioAvailable = false;
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String storeLocally(String bucket, String objectName, InputStream inputStream) {
        try {
            Path target = getLocalStorageDir().resolve(bucket).resolve(objectName);
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            localFallbackFiles.put(buildLocalKey(bucket, objectName), target);
            return "/api/storage/files/" + bucket + "/" + objectName.replace("\\", "/");
        } catch (IOException ex) {
            throw new IllegalStateException("Local fallback storage failed", ex);
        }
    }

    private Path resolveLocalPath(String bucket, String objectName) {
        Path cached = localFallbackFiles.get(buildLocalKey(bucket, objectName));
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        Path target = getLocalStorageDir().resolve(bucket).resolve(objectName);
        if (Files.exists(target)) {
            return target;
        }
        Path legacyTarget = Path.of(System.getProperty("java.io.tmpdir"), "edumind-storage")
                .resolve(bucket).resolve(objectName);
        if (Files.exists(legacyTarget)) {
            return legacyTarget;
        }
        return target;
    }

    private Path getLocalStorageDir() {
        if (localStorageDir != null) {
            return localStorageDir;
        }
        synchronized (this) {
            if (localStorageDir == null) {
                localStorageDir = determineLocalStorageDir();
                try {
                    Files.createDirectories(localStorageDir);
                } catch (IOException e) {
                    log.warn("Failed to create storage directory {}: {}", localStorageDir, e.getMessage());
                }
            }
        }
        return localStorageDir;
    }

    private Path determineLocalStorageDir() {
        if (customStorageDir != null && !customStorageDir.isBlank()) {
            return Path.of(customStorageDir);
        }
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (userDir.getFileName() != null && userDir.getFileName().toString().equalsIgnoreCase("backend")) {
            return userDir.resolve("data");
        }
        Path backendData = userDir.resolve("backend").resolve("data");
        if (Files.exists(backendData) || Files.exists(userDir.resolve("backend"))) {
            return backendData;
        }
        Path directData = userDir.resolve("data");
        if (Files.exists(directData)) {
            return directData;
        }
        Path current = userDir;
        while (current != null) {
            Path p = current.resolve("backend").resolve("data");
            if (Files.exists(p) || Files.exists(current.resolve("backend"))) {
                return p;
            }
            current = current.getParent();
        }
        return Path.of(System.getProperty("java.io.tmpdir"), "edumind-storage");
    }

    private String buildLocalKey(String bucket, String objectName) {
        return bucket + ":" + objectName;
    }

    private String buildMinioUrl(String bucket, String objectName) {
        String normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return normalizedEndpoint + "/" + bucket + "/" + objectName;
    }
}
