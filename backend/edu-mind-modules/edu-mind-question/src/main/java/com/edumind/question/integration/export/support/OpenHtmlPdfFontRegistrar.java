package com.edumind.question.integration.export.support;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenHTMLtoPDF + PDFBox 3 不再内置 Standard 14 字体，需用本地 TTF/TTC 覆盖全部别名。
 */
@Slf4j
public final class OpenHtmlPdfFontRegistrar {

    private static final List<String> FONT_FAMILY_ALIASES = List.of(
            "sans-serif", "serif", "monospace",
            "SimSun", "Songti SC", "Microsoft YaHei", "Arial",
            "Times", "Times New Roman", "Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic",
            "Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Helvetica-BoldOblique",
            "Courier", "Courier New", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique",
            "COURIER", "COURIER_BOLD", "COURIER_OBLIQUE", "COURIER_BOLD_OBLIQUE",
            "HELVETICA", "HELVETICA_BOLD", "HELVETICA_OBLIQUE", "HELVETICA_BOLD_OBLIQUE",
            "TIMES_ROMAN", "TIMES_BOLD", "TIMES_ITALIC", "TIMES_BOLD_ITALIC",
            "DejaVu Sans", "DejaVu Sans Mono"
    );

    private OpenHtmlPdfFontRegistrar() {
    }

    public static void register(PdfRendererBuilder builder) {
        File body = firstExisting(
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/msyh.ttf",
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/simsun.ttf",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        );
        File latin = firstExisting(
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/cour.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                body != null ? body.getAbsolutePath() : null
        );

        if (body == null && latin == null) {
            log.warn("[OpenHtmlPdfFontRegistrar] 未找到可用字体文件，PDF 排版将失败");
            return;
        }

        File primary = body != null ? body : latin;
        Set<String> families = new LinkedHashSet<>(FONT_FAMILY_ALIASES);
        for (String family : families) {
            registerNormalWeights(builder, primary, family);
        }

        if (latin != null && !latin.equals(primary)) {
            for (String family : List.of("Courier", "Courier New", "monospace", "COURIER_BOLD_OBLIQUE", "Courier-BoldOblique")) {
                registerNormalWeights(builder, latin, family);
            }
        }

        log.info("[OpenHtmlPdfFontRegistrar] primary font -> {}", primary.getAbsolutePath());
    }

    /** 仅注册 NORMAL 字重，避免 PDFBox 3 查找 COURIER_BOLD_OBLIQUE 等合成斜体失败 */
    private static void registerNormalWeights(PdfRendererBuilder builder, File font, String family) {
        for (int weight : new int[] {400, 700}) {
            try {
                builder.useFont(font, family, weight, FontStyle.NORMAL, true);
            } catch (Exception ex) {
                log.trace("[OpenHtmlPdfFontRegistrar] skip {} {} NORMAL: {}", family, weight, ex.getMessage());
            }
        }
        try {
            builder.useFont(font, family);
        } catch (Exception ex) {
            log.debug("[OpenHtmlPdfFontRegistrar] useFont({}) failed: {}", family, ex.getMessage());
        }
    }

    private static File firstExisting(String... paths) {
        List<String> normalized = new ArrayList<>();
        for (String path : paths) {
            if (path != null && !path.isBlank()) {
                normalized.add(path);
            }
        }
        for (String path : normalized) {
            File file = new File(path);
            if (file.isFile() && file.exists()) {
                return file;
            }
        }
        return null;
    }
}
