package com.edumind.infrastructure.oss;

import java.io.InputStream;

public interface FileStorageService {
    String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType);
    InputStream getFile(String bucketName, String objectName);
    void deleteFile(String bucketName, String objectName);

    default String getStorageType() {
        return "local";
    }

    default boolean testConnection() {
        return true;
    }
}