package com.edumind.common.event;

import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 业务操作日志记录事件，解耦各业务模块与日志持久化模块。
 */
@Getter
public class OperationLogEvent extends ApplicationEvent {

    private final OperationLogPayload payload;

    public OperationLogEvent(Object source, OperationLogPayload payload) {
        super(source);
        this.payload = payload;
    }

    @Getter
    @Builder
    public static class OperationLogPayload {
        private Long tenantId;
        private String title;
        private Integer businessType;
        private String method;
        private String requestMethod;
        private Long operUserId;
        private String operName;
        private String operUrl;
        private String operIp;
        private String operParam;
        private String jsonResult;
        private Integer status;
        private String errorMsg;
        private Long costTime;
        private LocalDateTime operTime;
    }
}
