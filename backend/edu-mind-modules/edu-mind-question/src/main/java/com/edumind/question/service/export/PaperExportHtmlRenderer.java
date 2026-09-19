package com.edumind.question.service.export;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.question.dto.export.PaperExportRequestDTO;
import com.edumind.question.integration.export.support.ExportMathHtmlConverter;
import com.edumind.question.integration.export.support.PaperExportSectionGrouper;
import com.edumind.question.integration.export.support.PaperExportSectionGrouper.ExportQuestion;
import com.edumind.question.integration.export.support.PaperExportSectionGrouper.ExportSection;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.teaching.vo.exam.ExamVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class PaperExportHtmlRenderer {

    public String render(PaperExportRequestDTO params, ExamVO exam) {
        List<ExportSection> sections = PaperExportSectionGrouper.group(exam);
        String title = StringUtils.hasText(params.getPaperTitle()) ? params.getPaperTitle() : exam.getTitle();
        String subtitle = params.getPaperSubtitle() != null ? params.getPaperSubtitle() : "";
        String watermark = Boolean.TRUE.equals(params.getShowWatermark()) && StringUtils.hasText(params.getWatermarkText())
                ? params.getWatermarkText() : "";

        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>");
        body.append(getCss());
        body.append("</style></head><body>");

        if (StringUtils.hasText(watermark)) {
            body.append("<div class=\"watermark\">").append(escape(watermark)).append("</div>");
        }

        body.append("<div class=\"page\">");
        if (Boolean.TRUE.equals(params.getShowSealingLine())) {
            body.append("<div class=\"seal\">密封线内严禁答题</div>");
        }
        body.append("<div class=\"content\">");
        if (StringUtils.hasText(params.getConfidentialLevel())) {
            body.append("<div class=\"conf\">").append(escape(params.getConfidentialLevel())).append("</div>");
        }
        body.append("<h1>").append(escape(title)).append("</h1>");
        if (StringUtils.hasText(subtitle)) {
            body.append("<h2>").append(escape(subtitle)).append("</h2>");
        }
        body.append("<div class=\"meta\">满分 ").append(exam.getTotalScore() != null ? exam.getTotalScore() : 0)
                .append(" 分 · 时长 ").append(exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 0)
                .append(" 分钟 · 共 ").append(countQuestions(sections)).append(" 题</div>");

        for (ExportSection section : sections) {
            body.append("<h3>").append(escape(section.getTitle()))
                    .append("（").append(section.getQuestions().size()).append(" 小题，")
                    .append(section.getTotalScore()).append(" 分）</h3>");
            for (ExportQuestion item : section.getQuestions()) {
                QuestionVO q = item.getQuestion();
                body.append("<div class=\"question\">");
                body.append("<p><strong>").append(item.getIndex()).append(".</strong> ");
                body.append(ExportMathHtmlConverter.renderMathText(q.getStem() != null ? q.getStem() : ""));
                if (Boolean.TRUE.equals(params.getShowPointBadge())) {
                    body.append(" <span class=\"score\">(").append(item.getScore()).append("分)</span>");
                }
                body.append("</p>");
                appendOptions(body, q, params.getOptionLayout());
                body.append("</div>");
            }
        }
        body.append("</div></div>");

        if (Boolean.TRUE.equals(params.getShowAnswerSheet())) {
            body.append(renderAnswerSheet(params, exam, sections));
        }

        if (Boolean.TRUE.equals(params.getShowAnalysis())) {
            body.append("<div class=\"page analysis\">");
            body.append("<h2>参考答案与解析</h2>");
            for (ExportSection section : sections) {
                for (ExportQuestion item : section.getQuestions()) {
                    QuestionVO q = item.getQuestion();
                    body.append("<div class=\"analysis-item\">");
                    body.append("<h4>第 ").append(item.getIndex()).append(" 题</h4>");
                    body.append("<p><strong>答案：</strong>")
                            .append(ExportMathHtmlConverter.renderMathText(q.getAnswer() != null ? q.getAnswer() : "略"))
                            .append("</p>");
                    if (StringUtils.hasText(q.getAnalysis())) {
                        body.append("<p><strong>解析：</strong>")
                                .append(ExportMathHtmlConverter.renderMathText(q.getAnalysis()))
                                .append("</p>");
                    }
                    body.append("</div>");
                }
            }
            body.append("</div>");
        }

        body.append("</body></html>");
        return body.toString();
    }

    private void appendOptions(StringBuilder body, QuestionVO q, String layout) {
        if (!StringUtils.hasText(q.getOptions())) {
            return;
        }
        try {
            JSONArray arr = JSON.parseArray(q.getOptions());
            if (arr == null || arr.isEmpty()) {
                return;
            }
            body.append("<div class=\"options layout-").append(layout != null ? layout : "horizontal").append("\">");
            for (int i = 0; i < arr.size(); i++) {
                Object raw = arr.get(i);
                String key = String.valueOf((char) ('A' + i));
                String content = "";
                if (raw instanceof JSONObject obj) {
                    key = obj.getString("key") != null ? obj.getString("key") : key;
                    content = obj.getString("content") != null ? obj.getString("content") : obj.getString("text");
                } else {
                    content = String.valueOf(raw);
                }
                body.append("<div class=\"opt\"><span>").append(escape(key)).append(". </span>");
                body.append(ExportMathHtmlConverter.renderMathText(content != null ? content : ""));
                body.append("</div>");
            }
            body.append("</div>");
        } catch (Exception ignored) {
            // options 可能为对象格式
        }
    }

    private int countQuestions(List<ExportSection> sections) {
        return sections.stream().mapToInt(s -> s.getQuestions().size()).sum();
    }

    private String renderAnswerSheet(PaperExportRequestDTO params, ExamVO exam, List<ExportSection> sections) {
        String title = StringUtils.hasText(params.getPaperTitle()) ? params.getPaperTitle() : exam.getTitle();
        int totalScore = exam.getTotalScore() != null ? exam.getTotalScore() : 0;
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page answer-sheet\">");
        sb.append("<p class=\"as-org\">普通高等学校招生全国统一考试 · 答题卡样式参考（A4）</p>");
        sb.append("<h1 class=\"as-title\">").append(escape(title)).append("</h1>");
        sb.append("<div class=\"as-meta\">满分 ").append(totalScore).append(" 分 · A4</div>");

        sb.append("<table class=\"as-header-table\"><tbody>");
        sb.append("<tr><td class=\"as-ht-info\">");
        sb.append("<p>姓名 <span class=\"ul wide\"></span></p>");
        sb.append("<p>考场号 <span class=\"ul mid\"></span> 座位号 <span class=\"ul short\"></span></p>");
        sb.append("</td><td class=\"as-ht-barcode\" rowspan=\"2\">");
        sb.append("<div class=\"as-barcode\">贴条形码区<br/><span class=\"sub\">勿贴出虚线框</span></div>");
        sb.append("<p class=\"as-absent\">缺考标记 <span class=\"slot\"></span></p>");
        sb.append("</td></tr>");
        sb.append("<tr><td class=\"as-ht-adm\">");
        sb.append("<div class=\"as-adm-grid\">");
        sb.append("<span class=\"as-adm-title\"><strong>准考证号</strong></span>");
        sb.append("<span class=\"as-adm-corner\"></span>");
        for (int col = 0; col < 10; col++) {
            sb.append("<span class=\"hw-cell\" style=\"grid-column:").append(col + 3).append(";grid-row:1\"></span>");
        }
        for (int d = 0; d < 10; d++) {
            int row = d + 2;
            sb.append("<span class=\"as-adm-digit\" style=\"grid-column:2;grid-row:").append(row).append("\">")
                    .append(d).append("</span>");
        }
        for (int col = 0; col < 10; col++) {
            int gridCol = col + 3;
            for (int d = 0; d < 10; d++) {
                int row = d + 2;
                sb.append("<span class=\"slot adm\" style=\"grid-column:").append(gridCol)
                        .append(";grid-row:").append(row).append("\"></span>");
            }
        }
        sb.append("</div></td></tr>");
        sb.append("<tr><td class=\"as-ht-notice\" colspan=\"2\"><strong>注意事项</strong><ol>");
        sb.append("<li>答题前填好姓名、准考证号、考场号、座位号，核对条形码后粘贴。</li>");
        sb.append("<li>选择题用 2B 铅笔涂满涂黑；非选择题用 0.5 毫米黑色签字笔作答。</li>");
        sb.append("<li>不得在条形码、图像定位点（黑方块）周围作涂写和标记。</li>");
        sb.append("</ol></td></tr></tbody></table>");

        List<ExportQuestion> objective = new ArrayList<>();
        List<ExportQuestion> fill = new ArrayList<>();
        List<ExportQuestion> shortAns = new ArrayList<>();
        for (ExportSection section : sections) {
            for (ExportQuestion item : section.getQuestions()) {
                String type = item.getQuestion().getType() != null ? item.getQuestion().getType() : "";
                if ("SINGLE_CHOICE".equals(type) || "MULTIPLE_CHOICE".equals(type) || "TRUE_FALSE".equals(type)) {
                    objective.add(item);
                } else if ("FILL_BLANK".equals(type)) {
                    fill.add(item);
                } else {
                    shortAns.add(item);
                }
            }
        }

        int block = 0;
        if (!objective.isEmpty()) {
            int maxOpt = objective.stream().mapToInt(q -> optionCount(q.getQuestion())).max().orElse(4);
            sb.append("<div class=\"as-scan\"><span class=\"mark tl\"></span><span class=\"mark tr\"></span>");
            sb.append("<span class=\"mark bl\"></span><span class=\"mark br\"></span>");
            sb.append("<h3 class=\"as-block\">").append(chinese(block++)).append("、选择题（用 2B 铅笔填涂）</h3>");
            sb.append("<table class=\"as-choice-table\"><thead><tr><th>题号</th>");
            for (int i = 0; i < maxOpt; i++) {
                sb.append("<th>").append((char) ('A' + i)).append("</th>");
            }
            sb.append("</tr></thead><tbody>");
            for (ExportQuestion item : objective) {
                sb.append("<tr><td class=\"qno\">").append(item.getIndex()).append("</td>");
                int optCount = optionCount(item.getQuestion());
                for (int i = 0; i < maxOpt; i++) {
                    sb.append("<td>");
                    if (i < optCount) {
                        sb.append("<span class=\"slot choice\"></span>");
                    }
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("</tbody></table></div>");
        }
        if (!fill.isEmpty()) {
            sb.append("<h3 class=\"as-block\">").append(chinese(block++)).append("、填空题（黑色签字笔）</h3>");
            for (ExportQuestion item : fill) {
                sb.append("<p class=\"as-fill\">").append(item.getIndex()).append(". <span class=\"ul fill\"></span></p>");
            }
        }
        if (!shortAns.isEmpty()) {
            sb.append("<h3 class=\"as-block\">").append(chinese(block)).append("、解答题（矩形框内作答）</h3>");
            for (ExportQuestion item : shortAns) {
                sb.append("<div class=\"as-frame\">");
                sb.append("<div class=\"as-frame-head\">").append(item.getIndex()).append(" 题 · 评卷人 <span class=\"score-box\"></span> 得分 <span class=\"score-box wide\"></span></div>");
                sb.append("<div class=\"as-frame-body\"></div></div>");
            }
        }

        sb.append("<div class=\"as-foot\">版式参考新高考 A4 答题卡（各省考试院公开说明）· 正面</div>");
        sb.append("</div>");
        return sb.toString();
    }

    private int optionCount(QuestionVO q) {
        if (!StringUtils.hasText(q.getOptions())) {
            return 4;
        }
        try {
            JSONArray arr = JSON.parseArray(q.getOptions());
            if (arr == null || arr.isEmpty()) {
                return 4;
            }
            return Math.min(Math.max(arr.size(), 2), 7);
        } catch (Exception e) {
            return 4;
        }
    }

    private String chinese(int index) {
        String[] labels = {"一", "二", "三", "四", "五", "六", "七", "八"};
        return index < labels.length ? labels[index] : String.valueOf(index + 1);
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String getCss() {
        return """
                @page { size: A4; margin: 12mm; }
                *, *::before, *::after {
                  font-family: SimSun, 'Microsoft YaHei', 'Songti SC', serif !important;
                  font-style: normal !important;
                }
                strong, b { font-weight: 700 !important; font-style: normal !important; }
                html, body, pre, code, kbd, tt, samp, var, .math-fallback {
                  font-family: SimSun, 'Microsoft YaHei', 'Songti SC', serif !important;
                  font-style: normal !important;
                  font-size: 12pt; color: #111; }
                code, .math-fallback { font-size: 10.5pt; }
                em, i, cite { font-style: normal !important; }
                .page { position: relative; page-break-after: always; }
                .watermark { position: fixed; top: 40%; left: 10%; transform: rotate(-30deg);
                  font-size: 28pt; color: rgba(150,150,150,0.25); z-index: -1; }
                .seal { writing-mode: vertical-rl; float: left; border-right: 1px dashed #333;
                  padding-right: 8px; margin-right: 12px; font-size: 10pt; }
                .content { margin-left: 8px; }
                h1 { text-align: center; font-size: 18pt; margin: 0 0 6px; }
                h2 { text-align: center; font-size: 12pt; font-weight: normal; margin: 0 0 10px; }
                .meta { text-align: center; border-top: 1px solid #000; border-bottom: 1px solid #000;
                  padding: 4px 0; margin-bottom: 14px; font-size: 10pt; }
                h3 { font-size: 12pt; margin: 12px 0 6px; }
                .question { margin-bottom: 10px; }
                .score { font-size: 10pt; color: #444; }
                .options { margin: 4px 0 0 16px; }
                .options.layout-horizontal { display: table; width: 100%; }
                .options.layout-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; }
                .opt { margin-bottom: 4px; }
                .analysis-item { margin-bottom: 12px; border-bottom: 1px solid #eee; padding-bottom: 8px;
                  line-height: 1.75; word-break: normal; text-align: left; }
                .analysis-item p { word-break: normal; overflow-wrap: anywhere; text-align: left; }
                .analysis-item img[alt="math"] { display: inline-block; vertical-align: middle;
                  max-width: none; height: 1.05em; margin: 0 1px; }
                .analysis-item .math-block { display: block; margin: 6px 0; }
                .analysis-item code { display: inline; white-space: normal; word-break: normal; }
                .answer-sheet { position: relative; padding-top: 4mm; }
                .as-org { text-align: center; font-size: 8pt; margin: 0 0 2mm; color: #333; }
                .as-title { text-align: center; font-size: 14pt; margin: 0 0 2mm; }
                .as-meta { text-align: center; border-top: 1px solid #000; border-bottom: 1px solid #000;
                  padding: 2mm 0; font-size: 9pt; margin-bottom: 4mm; }
                .as-header-table { width: 100%; border-collapse: collapse; border: 1.5px solid #000; margin-bottom: 4mm; }
                .as-header-table td { border: 1px solid #000; padding: 2mm; vertical-align: top; font-size: 9pt; }
                .as-ht-barcode { width: 32mm; text-align: center; }
                .as-barcode { min-height: 14mm; border: 1px dashed #666; font-size: 8pt; padding: 2mm; }
                .as-barcode .sub { font-size: 7pt; color: #555; }
                .as-absent { margin-top: 2mm; font-size: 8pt; }
                .as-ht-notice { font-size: 7.5pt; }
                .as-ht-notice ol { margin: 1mm 0 0; padding-left: 4mm; }
                .ul { display: inline-block; border-bottom: 1px solid #000; height: 12px; vertical-align: bottom; }
                .ul.wide { width: 50mm; } .ul.mid { width: 18mm; } .ul.short { width: 12mm; } .ul.fill { width: 120mm; }
                .as-ht-adm { text-align: right; }
                .as-adm-grid { display: grid; grid-template-columns: auto 3mm repeat(10, 3.5mm);
                  grid-template-rows: 5mm repeat(10, 2.3mm); column-gap: 1mm; row-gap: 0;
                  align-items: start; width: fit-content; max-width: 100%; margin-left: auto; }
                .as-adm-title { grid-column: 1; grid-row: 1; align-self: end; padding-bottom: 0.3mm; white-space: nowrap; }
                .as-adm-corner { grid-column: 2; grid-row: 1; }
                .as-adm-digit { width: 3mm; height: 2.3mm; font-size: 6pt; line-height: 2.3mm;
                  text-align: right; justify-self: end; align-self: center; }
                .hw-cell { width: 3.5mm; height: 5mm; border: 1px solid #000; box-sizing: border-box;
                  justify-self: center; align-self: end; }
                .slot { display: inline-block; width: 3mm; height: 2mm; border: 1px solid #000; border-radius: 0.3mm; background: #fff; }
                .slot.adm { width: 3.5mm; height: 2.2mm; justify-self: center; align-self: center; }
                .slot.choice { width: 3.5mm; height: 2.2mm; }
                .as-scan { position: relative; border: 1px solid #000; padding: 3mm; margin-bottom: 3mm; }
                .mark { position: absolute; width: 2.5mm; height: 2.5mm; background: #000; }
                .mark.tl { top: -0.2mm; left: -0.2mm; } .mark.tr { top: -0.2mm; right: -0.2mm; }
                .mark.bl { bottom: -0.2mm; left: -0.2mm; } .mark.br { bottom: -0.2mm; right: -0.2mm; }
                .as-block { font-size: 10pt; text-align: center; margin: 0 0 2mm; font-weight: bold; }
                .as-choice-table { width: 100%; border-collapse: collapse; font-size: 8pt; }
                .as-choice-table th, .as-choice-table td { border: 1px solid #999; text-align: center; padding: 1mm; }
                .as-choice-table .qno { font-weight: bold; width: 8mm; }
                .as-choice-table thead th { background: #f3f4f6; }
                .as-fill { margin: 4px 0; }
                .as-frame { border: 1px solid #000; margin-bottom: 6px; }
                .as-frame-head { border-bottom: 1px solid #000; padding: 3px 6px; font-size: 9pt; }
                .score-box { display: inline-block; width: 8mm; height: 6mm; border: 1px solid #000; vertical-align: middle; }
                .score-box.wide { width: 14mm; }
                .as-frame-body { min-height: 28mm; }
                .as-foot { font-size: 8pt; color: #555; border-top: 1px solid #ccc; margin-top: 6px; padding-top: 4px; }
                """;
    }
}
