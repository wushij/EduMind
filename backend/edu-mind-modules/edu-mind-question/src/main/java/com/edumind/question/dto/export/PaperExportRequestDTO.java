package com.edumind.question.dto.export;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 试卷导出生成请求 DTO
 */
@Data
public class PaperExportRequestDTO {

    /**
     * 关联试卷/题集 ID
     */
    @NotNull(message = "试卷 ID 不能为空")
    private Long examId;

    /**
     * 试卷主标题
     */
    private String paperTitle;

    /**
     * 试卷副标题
     */
    private String paperSubtitle;

    /**
     * 纸张幅面 (A4 / B4)
     */
    private String paperSize;

    /**
     * 是否开启防伪水印
     */
    private Boolean showWatermark;

    /**
     * 水印自定义文字
     */
    private String watermarkText;

    /**
     * 是否附带标准机读答题卡
     */
    private Boolean showAnswerSheet;

    /**
     * 是否附带名师考点解析与评分细则
     */
    private Boolean showAnalysis;

    /**
     * 是否显示考生考号条码填涂区
     */
    private Boolean showStudentInfo;

    /**
     * 是否显示大题得分统分网格栏
     */
    private Boolean showScoreGrid;

    private String confidentialLevel;

    private Boolean showSealingLine;

    private Boolean showNoticeBar;

    private Boolean showPointBadge;

    private String fontFamily;

    private String lineSpacing;

    private String optionLayout;
}
