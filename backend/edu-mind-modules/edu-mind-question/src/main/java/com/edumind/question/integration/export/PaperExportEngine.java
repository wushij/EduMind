package com.edumind.question.integration.export;

/**
 * 试卷排版导出引擎统一适配接口
 */
public interface PaperExportEngine {

    /**
     * 执行试卷生成与排版导出
     */
    PaperExportResult export(PaperExportRequest request);
}
