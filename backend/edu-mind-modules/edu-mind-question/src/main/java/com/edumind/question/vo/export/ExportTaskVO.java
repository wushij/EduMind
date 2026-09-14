package com.edumind.question.vo.export;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导出任务展示视图对象 VO
 */
@Data
public class ExportTaskVO implements Serializable {

    private String taskId;

    private Long tenantId;

    private Long userId;

    private String bizType;

    private Long bizId;

    private String status;

    private Integer progress;

    private String downloadUrl;

    private String errorMsg;

    private LocalDateTime createTime;
}
