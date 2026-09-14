package com.edumind.question.service.export;

import com.edumind.common.context.TenantContext;
import com.edumind.common.utils.TenantObjectKeyBuilder;
import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.question.dao.export.ExportTaskDao;
import com.edumind.question.entity.export.ExportTaskEntity;
import com.edumind.question.integration.export.PaperExportEngine;
import com.edumind.question.integration.export.PaperExportRequest;
import com.edumind.question.integration.export.PaperExportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * 试卷导出任务异步调度器 (状态机: PENDING -> PROCESSING -> SUCCESS / FAILED)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskDispatcher {

    private final ExportTaskDao exportTaskDao;
    private final PaperExportEngine paperExportEngine;
    private final FileStorageService fileStorageService;

    @Value("${minio.bucketName:edumind}")
    private String bucketName;

    @Async("questionTaskExecutor")
    public void dispatchAsync(Long taskId, Long tenantId) {
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            ExportTaskEntity task = null;
            for (int i = 0; i < 5; i++) {
                task = exportTaskDao.findById(taskId);
                if (task != null) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (task == null) {
                log.warn("[Export Dispatcher] Task not found: {}", taskId);
                return;
            }
            if (!"PENDING".equalsIgnoreCase(task.getStatus())) {
                log.info("[Export Dispatcher] Task {} status is {}, skipping dispatch", taskId, task.getStatus());
                return;
            }

            // 1. 转为 PROCESSING 状态
            task.setStatus("PROCESSING");
            exportTaskDao.updateById(task);

            // 2. 构造请求调用引擎适配器
            PaperExportRequest req = PaperExportRequest.builder()
                    .taskId(task.getId())
                    .tenantId(task.getTenantId())
                    .userId(task.getUserId())
                    .examId(task.getBizId())
                    .exportParamsJson(task.getExportParams())
                    .build();

            PaperExportResult result = paperExportEngine.export(req);

            // 3. 上传 OSS 对象存储
            String objectKey = TenantObjectKeyBuilder.exportFile(tenantId, task.getId(), result.getFilename());
            try (ByteArrayInputStream is = new ByteArrayInputStream(result.getFileBytes())) {
                fileStorageService.uploadFile(bucketName, objectKey, is, result.getContentType());
            }

            // 4. 生成鉴权短链并持久化
            String downloadToken = UUID.randomUUID().toString().replace("-", "");
            String downloadUrl = "/api/question/exports/" + task.getId() + "/download?token=" + downloadToken;

            task.setDownloadToken(downloadToken);
            task.setObjectKey(objectKey);
            task.setFileUrl(downloadUrl);
            task.setStatus("SUCCESS");
            exportTaskDao.updateById(task);

            log.info("[Export Dispatcher] Task {} export completed successfully, objectKey: {}", taskId, objectKey);

        } catch (Exception e) {
            log.error("[Export Dispatcher] Task {} export failed: {}", taskId, e.getMessage(), e);
            try {
                ExportTaskEntity task = exportTaskDao.findById(taskId);
                if (task != null) {
                    task.setStatus("FAILED");
                    String msg = e.getMessage();
                    task.setErrorMsg(msg != null ? (msg.length() > 500 ? msg.substring(0, 500) : msg) : "试卷导出处理异常");
                    exportTaskDao.updateById(task);
                }
            } catch (Exception ex) {
                log.error("[Export Dispatcher] Failed to mark task {} as FAILED: {}", taskId, ex.getMessage());
            }
        } finally {
            if (tenantId != null) {
                TenantContext.clear();
            }
        }
    }
}
