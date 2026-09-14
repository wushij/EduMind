package com.edumind.question.integration.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 试卷导出生成请求内部模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperExportRequest {

    private Long taskId;

    private Long tenantId;

    private Long userId;

    private Long examId;

    private String paperTitle;

    private String paperSubtitle;

    private String paperSize;

    private String exportParamsJson;
}
