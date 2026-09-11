package com.edumind.teaching.vo.exam;

import lombok.Data;

/**
 * 试卷导出预览包（V0.2 JSON 格式；PDF 导出归后续版本）
 */
@Data
public class ExamExportVO {

    private String exportFormat = "JSON";

    private ExamVO exam;
}
