package com.edumind.system.listener;

import com.edumind.common.event.OperationLogEvent;
import com.edumind.system.entity.SysOperLogEntity;
import com.edumind.system.service.log.SysOperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步操作日志事件监听器，解耦落库过程，保障主业务高性能与事务独立
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogListener {

    private final SysOperLogService sysOperLogService;

    @Async
    @EventListener
    public void onOperationLogEvent(OperationLogEvent event) {
        if (event == null || event.getPayload() == null) {
            return;
        }

        OperationLogEvent.OperationLogPayload p = event.getPayload();
        try {
            SysOperLogEntity entity = SysOperLogEntity.builder()
                    .tenantId(p.getTenantId())
                    .title(p.getTitle())
                    .businessType(p.getBusinessType())
                    .method(p.getMethod())
                    .requestMethod(p.getRequestMethod())
                    .operUserId(p.getOperUserId())
                    .operName(p.getOperName())
                    .operUrl(p.getOperUrl())
                    .operIp(p.getOperIp())
                    .operParam(p.getOperParam())
                    .jsonResult(p.getJsonResult())
                    .status(p.getStatus())
                    .errorMsg(p.getErrorMsg())
                    .costTime(p.getCostTime())
                    .operTime(p.getOperTime())
                    .build();

            sysOperLogService.recordLog(entity);
        } catch (Exception e) {
            log.error("异步落库操作日志失败", e);
        }
    }
}
