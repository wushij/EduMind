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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private static final long EXPORT_TIMEOUT_MINUTES = 5;

    @Async("questionTaskExecutor")
    public void dispatchAsync(Long taskId, Long tenantId) {
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            ExportTaskEntity task = waitForTask(taskId);
            if (task == null) {
                log.warn("[Export Dispatcher] Task not found: {}", taskId);
                return;
            }
            if (!"PENDING".equalsIgnoreCase(task.getStatus())) {
                log.info("[Export Dispatcher] Task {} status is {}, skipping dispatch", taskId, task.getStatus());
                return;
            }

            markProgress(task, "PROCESSING", 10);

            PaperExportRequest req = PaperExportRequest.builder()
                    .taskId(task.getId())
                    .tenantId(task.getTenantId())
                    .userId(task.getUserId())
                    .examId(task.getBizId())
                    .exportParamsJson(task.getExportParams())
                    .build();

            Long exportTenantId = task.getTenantId();
            PaperExportResult result = runWithTimeout(
                    () -> paperExportEngine.export(req),
                    EXPORT_TIMEOUT_MINUTES,
                    TimeUnit.MINUTES,
                    "PDF 排版超时（超过 " + EXPORT_TIMEOUT_MINUTES + " 分钟），请减少题量或关闭答题卡/解析后重试",
                    exportTenantId
            );

            markProgress(task, "PROCESSING", 75);

            String objectKey = TenantObjectKeyBuilder.exportFile(tenantId, task.getId(), result.getFilename());
            byte[] fileBytes = result.getFileBytes();
            runWithTimeout(
                    () -> {
                        try (ByteArrayInputStream is = new ByteArrayInputStream(fileBytes)) {
                            fileStorageService.uploadFile(bucketName, objectKey, is, result.getContentType());
                        }
                        return null;
                    },
                    2,
                    TimeUnit.MINUTES,
                    "文件上传超时，请检查 MinIO/本地存储配置",
                    exportTenantId
            );

            markProgress(task, "PROCESSING", 95);

            String downloadToken = UUID.randomUUID().toString().replace("-", "");
            String downloadUrl = "/api/question/exports/" + task.getId() + "/download?token=" + downloadToken;

            task.setDownloadToken(downloadToken);
            task.setObjectKey(objectKey);
            task.setFileUrl(downloadUrl);
            markProgress(task, "SUCCESS", 100);

            log.info("[Export Dispatcher] Task {} export completed successfully, objectKey: {}", taskId, objectKey);

        } catch (Exception e) {
            log.error("[Export Dispatcher] Task {} export failed: {}", taskId, e.getMessage(), e);
            markFailed(taskId, e);
        } finally {
            TenantContext.clear();
        }
    }

    private ExportTaskEntity waitForTask(Long taskId) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            ExportTaskEntity task = exportTaskDao.findByIdIgnoreTenant(taskId);
            if (task != null) {
                return task;
            }
            Thread.sleep(50);
        }
        return exportTaskDao.findByIdIgnoreTenant(taskId);
    }

    private void markProgress(ExportTaskEntity task, String status, int progress) {
        task.setStatus(status);
        task.setProgress(Math.max(0, Math.min(progress, 100)));
        exportTaskDao.updateById(task);
    }

    private void markFailed(Long taskId, Exception e) {
        try {
            ExportTaskEntity task = exportTaskDao.findByIdIgnoreTenant(taskId);
            if (task != null) {
                task.setStatus("FAILED");
                String msg = e.getMessage();
                task.setErrorMsg(msg != null ? (msg.length() > 500 ? msg.substring(0, 500) : msg) : "试卷导出处理异常");
                exportTaskDao.updateById(task);
            }
        } catch (Exception ex) {
            log.error("[Export Dispatcher] Failed to mark task {} as FAILED: {}", taskId, ex.getMessage());
        }
    }

    /**
     * 导出引擎/上传在独立线程执行，必须传递租户上下文，否则 ExamQueryApi 查不到试卷。
     */
    private <T> T runWithTimeout(
            Callable<T> callable,
            long timeout,
            TimeUnit unit,
            String timeoutMessage,
            Long tenantId
    ) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "export-worker-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        try {
            Future<T> future = executor.submit(() -> {
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }
                try {
                    return callable.call();
                } finally {
                    TenantContext.clear();
                }
            });
            return future.get(timeout, unit);
        } catch (TimeoutException te) {
            throw new IllegalStateException(timeoutMessage);
        } finally {
            executor.shutdownNow();
        }
    }
}
