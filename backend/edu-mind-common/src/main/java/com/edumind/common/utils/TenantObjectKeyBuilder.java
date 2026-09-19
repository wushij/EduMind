package com.edumind.common.utils;

import com.edumind.common.context.TenantContext;

/**
 * 多租户文件存储 ObjectKey 规范化生成与归属权校验工具类
 */
public final class TenantObjectKeyBuilder {

    public static final String TENANT_PREFIX = "tenants/";

    private TenantObjectKeyBuilder() {
    }

    /**
     * 规范化构建租户 ObjectKey:
     * 若 tenantId 或上下文存在租户，返回：tenants/{tenantId}/{relativePath}
     * 否则返回：{relativePath}
     */
    public static String build(Long tenantId, String relativePath) {
        Long tid = tenantId != null ? tenantId : TenantContext.getTenantId();
        String cleanPath = relativePath != null ? relativePath.trim() : "";
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        if (tid != null && tid > 0) {
            return TENANT_PREFIX + tid + "/" + cleanPath;
        }
        return cleanPath;
    }

    /**
     * 知识库文档路径：tenants/{tenantId}/knowledge/{knowledgeBaseId}/{uuid}/{filename}
     */
    public static String knowledgeDocument(Long tenantId, Long knowledgeBaseId, String uuid, String filename) {
        return build(tenantId, "knowledge/" + knowledgeBaseId + "/" + uuid + "/" + sanitizeFilename(filename));
    }

    /**
     * 用户头像路径：tenants/{tenantId}/users/{userId}/avatar/{filename}
     */
    public static String userAvatar(Long tenantId, Long userId, String filename) {
        return build(tenantId, "users/" + userId + "/avatar/" + sanitizeFilename(filename));
    }

    /**
     * 导出文件路径：tenants/{tenantId}/export/{taskId}/{filename}
     */
    public static String exportFile(Long tenantId, Long taskId, String filename) {
        return build(tenantId, "export/" + taskId + "/" + sanitizeFilename(filename));
    }

    /**
     * 课程教学资料路径：tenants/{tenantId}/course/{courseId}/resources/{uuid}/{filename}
     */
    public static String courseTeachingResource(Long tenantId, Long courseId, String uuid, String filename) {
        return build(tenantId, "course/" + courseId + "/resources/" + uuid + "/" + sanitizeFilename(filename));
    }

    /**
     * 构建可通过 StorageFileController 访问的相对 URL
     */
    public static String storageFileUrl(String bucketName, String objectKey) {
        String bucket = bucketName != null && !bucketName.isBlank() ? bucketName : "edumind";
        String key = objectKey != null ? objectKey.trim() : "";
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return "/api/storage/files/" + bucket + "/" + key;
    }

    /**
     * 校验当前租户是否拥有指定 objectKey 的读写权限
     * 1. 若为旧版历史文件（不含 tenants/ 前缀），允许兼容访问
     * 2. 若当前为平台旁路 (currentTenantId == null 或 isIgnoreTenant)，允许全局访问
     * 3. 若为 tenants/{keyTenantId}/...，则 keyTenantId 必须等于 currentTenantId
     */
    public static boolean validateTenantOwnership(Long currentTenantId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }
        String cleanKey = objectKey.trim();
        while (cleanKey.startsWith("/")) {
            cleanKey = cleanKey.substring(1);
        }
        if (!cleanKey.startsWith(TENANT_PREFIX)) {
            return true;
        }
        if (TenantContext.isIgnoreTenant() || currentTenantId == null || currentTenantId <= 0) {
            return true;
        }
        String sub = cleanKey.substring(TENANT_PREFIX.length());
        int slashIdx = sub.indexOf('/');
        if (slashIdx <= 0) {
            return false;
        }
        try {
            Long keyTenantId = Long.parseLong(sub.substring(0, slashIdx));
            return currentTenantId.equals(keyTenantId);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replace("\\", "/").replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
