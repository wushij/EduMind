package com.edumind.question.integration.export.support;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
/**
 * 将题目文本中的 LaTeX 片段转为 HTML（内联 PNG），规则对齐前端 render-math.ts
 */
public final class ExportMathHtmlConverter {

    private ExportMathHtmlConverter() {
    }

    public static String renderMathText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = repairFracControlChar(text.replace("\\r\\n", "\n").replaceAll("\\\\n(?![a-zA-Z])", "\n"));
        List<Segment> segments = splitMathSegments(normalized);
        StringBuilder html = new StringBuilder();
        for (Segment seg : segments) {
            if (seg.display) {
                html.append("<div class=\"math-block\">").append(renderLatexToImg(seg.value, true)).append("</div>");
            } else if (seg.math) {
                html.append(renderLatexToImg(seg.value, false));
            } else {
                html.append(escapeHtml(seg.value.replaceAll("\\n{2,}", "\n").replace("\n", " ")));
            }
        }
        return html.toString();
    }

    private static String renderLatexToImg(String latex, boolean display) {
        try {
            TeXFormula formula = new TeXFormula(latex.trim());
            int size = display ? 20 : 16;
            TeXIcon icon = formula.createTeXIcon(display ? TeXConstants.STYLE_DISPLAY : TeXConstants.STYLE_TEXT, size);
            icon.setInsets(new java.awt.Insets(2, 2, 2, 2));
            BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
            icon.paintIcon(new JLabel(), g2, 0, 0);
            g2.dispose();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            String style = display
                    ? "display:block;margin:6px 0;max-width:100%;"
                    : "display:inline-block;vertical-align:middle;margin:0 1px;height:1.05em;";
            return "<img alt=\"math\" style=\"" + style + "\" src=\"data:image/png;base64," + b64 + "\"/>";
        } catch (Exception ex) {
            return "<span class=\"math-fallback\">" + escapeHtml(latex) + "</span>";
        }
    }

    private static List<Segment> splitMathSegments(String text) {
        List<Segment> segments = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            if (text.startsWith("$$", i)) {
                int end = text.indexOf("$$", i + 2);
                if (end != -1) {
                    segments.add(Segment.math(text.substring(i + 2, end), true));
                    i = end + 2;
                    continue;
                }
            }
            if (text.startsWith("\\[", i)) {
                int end = text.indexOf("\\]", i + 2);
                if (end != -1) {
                    segments.add(Segment.math(text.substring(i + 2, end), true));
                    i = end + 2;
                    continue;
                }
            }
            if (text.startsWith("\\(", i)) {
                int end = text.indexOf("\\)", i + 2);
                if (end != -1) {
                    segments.add(Segment.math(text.substring(i + 2, end), false));
                    i = end + 2;
                    continue;
                }
            }
            if (text.charAt(i) == '$' && i + 1 < text.length() && text.charAt(i + 1) != '$') {
                int end = text.indexOf('$', i + 1);
                if (end != -1) {
                    segments.add(Segment.math(text.substring(i + 1, end), false));
                    i = end + 1;
                    continue;
                }
            }
            int next = text.length();
            for (String marker : new String[] { "$$", "\\[", "\\(", "$" }) {
                int pos = text.indexOf(marker, i + 1);
                if (pos != -1 && pos < next) {
                    next = pos;
                }
            }
            String chunk = text.substring(i, next);
            if (!chunk.isEmpty()) {
                segments.add(Segment.text(chunk));
            }
            i = next;
        }
        return segments;
    }

    /** JSON 中 \\frac 若少转义会变成 \\f(Form Feed)+rac，JLatexMath 无法渲染 */
    private static String repairFracControlChar(String text) {
        return text.replace("\u000Crac{", "\\frac{");
    }

    private static String escapeHtml(String raw) {
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record Segment(boolean math, boolean display, String value) {
        static Segment text(String v) {
            return new Segment(false, false, v);
        }

        static Segment math(String v, boolean display) {
            return new Segment(true, display, v);
        }
    }
}
