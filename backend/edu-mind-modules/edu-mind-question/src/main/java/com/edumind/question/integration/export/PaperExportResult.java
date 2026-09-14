package com.edumind.question.integration.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 试卷导出生成结果模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperExportResult {

    private byte[] fileBytes;

    private String filename;

    private String contentType;
}
