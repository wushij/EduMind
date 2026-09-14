package com.edumind.infrastructure.oss.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.common.utils.TenantObjectKeyBuilder;
import com.edumind.infrastructure.oss.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘文件存储驱动实现
 */
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path localStorageDir;

    public LocalFileStorageService(String customPath) {
        this.localStorageDir = resolveStorageDir(customPath);
        try {
            Files.createDirectories(this.localStorageDir);
        } catch (IOException e) {
            log.warn("初始化本地存储目录失败 {}: {}", this.localStorageDir, e.getMessage());
        }
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName : "edumind";
        try {
            Path target = resolvePath(bucket, objectName);
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return "/api/storage/files/" + bucket + "/" + objectName.replace("\\", "/");
        } catch (IOException e) {
            throw new IllegalStateException("本地磁盘写入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getFile(String bucketName, String objectName) {
        Long currentTenantId = TenantContext.getTenantId();
        if (!TenantObjectKeyBuilder.validateTenantOwnership(currentTenantId, objectName)) {
            log.warn("拒绝跨租户读取本地存储文件: tenantId={}, objectName={}", currentTenantId, objectName);
            return null;
        }
        String bucket = StringUtils.hasText(bucketName) ? bucketName : "edumind";
        Path target = resolvePath(bucket, objectName);
        if (Files.exists(target)) {
            try {
                return Files.newInputStream(target);
            } catch (IOException e) {
                log.warn("本地文件读取失败: {}", e.getMessage());
                return null;
            }
        }
        // 回退兼容多目录搜索 (backend/data 与 根目录 data)
        Path backendTarget = Path.of("backend", "data").resolve(bucket).resolve(objectName).toAbsolutePath();
        if (Files.exists(backendTarget)) {
            try {
                return Files.newInputStream(backendTarget);
            } catch (IOException ignored) {
            }
        }
        Path rootDataTarget = Path.of("data").resolve(bucket).resolve(objectName).toAbsolutePath();
        if (Files.exists(rootDataTarget)) {
            try {
                return Files.newInputStream(rootDataTarget);
            } catch (IOException ignored) {
            }
        }
        // 回退兼容旧临时路径
        Path legacyTarget = Path.of(System.getProperty("java.io.tmpdir"), "edumind-storage")
                .resolve(bucket).resolve(objectName);
        if (Files.exists(legacyTarget)) {
            try {
                return Files.newInputStream(legacyTarget);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        Long currentTenantId = TenantContext.getTenantId();
        if (!TenantObjectKeyBuilder.validateTenantOwnership(currentTenantId, objectName)) {
            log.warn("拒绝跨租户删除本地存储文件: tenantId={}, objectName={}", currentTenantId, objectName);
            return;
        }
        String bucket = StringUtils.hasText(bucketName) ? bucketName : "edumind";
        Path target = resolvePath(bucket, objectName);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("本地文件删除异常: {}", e.getMessage());
        }
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    @Override
    public boolean testConnection() {
        try {
            Path testFile = localStorageDir.resolve(".test_probe");
            Files.writeString(testFile, "test");
            Files.deleteIfExists(testFile);
            return true;
        } catch (Exception e) {
            log.warn("本地存储连通性自测失败: {}", e.getMessage());
            return false;
        }
    }

    public String getStoragePath() {
        return this.localStorageDir.toAbsolutePath().normalize().toString().replace("\\", "/");
    }

    private Path resolvePath(String bucket, String objectName) {
        return localStorageDir.resolve(bucket).resolve(objectName);
    }

    private Path resolveStorageDir(String customPath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (userDir.getFileName() != null && userDir.getFileName().toString().equalsIgnoreCase("edu-mind-boot")) {
            Path parent = userDir.getParent();
            if (parent != null && parent.getFileName() != null && parent.getFileName().toString().equalsIgnoreCase("backend")) {
                userDir = parent;
            }
        }
        if (StringUtils.hasText(customPath)) {
            Path p = Path.of(customPath);
            if (p.isAbsolute()) {
                return p;
            }
            String normalized = customPath.replace("\\", "/");
            if (userDir.getFileName() != null && userDir.getFileName().toString().equalsIgnoreCase("backend")) {
                if (normalized.startsWith("backend/")) {
                    normalized = normalized.substring("backend/".length());
                }
                return userDir.resolve(normalized);
            }
            return userDir.resolve(normalized);
        }
        if (userDir.getFileName() != null && userDir.getFileName().toString().equalsIgnoreCase("backend")) {
            return userDir.resolve("data");
        }
        Path backendData = userDir.resolve("backend").resolve("data");
        if (Files.exists(backendData) || Files.exists(userDir.resolve("backend"))) {
            return backendData;
        }
        return userDir.resolve("data");
    }
}
