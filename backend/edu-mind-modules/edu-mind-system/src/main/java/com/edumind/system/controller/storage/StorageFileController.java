package com.edumind.system.controller.storage;

import com.edumind.common.context.TenantContext;
import com.edumind.common.utils.TenantObjectKeyBuilder;
import com.edumind.infrastructure.oss.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.InputStream;
import java.net.URLConnection;

/**
 * 本地/对象存储静态文件开放访问控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class StorageFileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/api/storage/files/{bucket}/**")
    public void getStorageFile(@PathVariable("bucket") String bucket,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String prefix = "/api/storage/files/" + bucket + "/";
        String objectName = "";
        if (path != null && path.startsWith(prefix)) {
            objectName = path.substring(prefix.length());
        }

        if (objectName.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Long currentTenantId = TenantContext.getTenantId();
        if (!TenantObjectKeyBuilder.validateTenantOwnership(currentTenantId, objectName)) {
            log.warn("拒绝跨租户下载存储文件: tenantId={}, bucket={}, objectName={}", currentTenantId, bucket, objectName);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try (InputStream inputStream = fileStorageService.getFile(bucket, objectName)) {
            if (inputStream == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String contentType = URLConnection.guessContentTypeFromName(objectName);
            if (contentType == null) {
                if (objectName.endsWith(".webp")) {
                    contentType = "image/webp";
                } else if (objectName.endsWith(".svg")) {
                    contentType = "image/svg+xml";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=86400");
            StreamUtils.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            log.warn("读取本地存储文件异常: bucket={}, objectName={}, error={}", bucket, objectName, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
