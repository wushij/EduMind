package com.edumind.common.markdown;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题干 / 解析文本的 LaTeX 归一化工具（大模型输出兜底）。
 *
 * <p>模型的实际输出很不稳定，常见的两种"不可渲染"写法：</p>
 * <ol>
 *   <li><b>裸 LaTeX</b>：{@code 计算极限：\lim_{x \to 0} \frac{e^x-1-x}{x^2}}（缺少 $ 定界符，KaTeX 不认）；</li>
 *   <li><b>纯文本/Unicode 数学</b>：{@code 极限 lim_{x→0} (tan x - sin x)/x^3 的值为（ ）}（无命令、无定界符）。</li>
 * </ol>
 *
 * <p>处理策略（保守、片段级）：</p>
 * <ul>
 *   <li>片段起点 = {@code \命令} 或独立函数名（lim/sin/tan/ln…）；</li>
 *   <li>片段内容 = 连续的 ASCII 与常见 Unicode 数学符号，遇到中文/全角标点/换行/美元符即终止；</li>
 *   <li>片段内做轻量 LaTeX 化：函数名补反斜杠、Unicode 数学符号转命令、多位上标加花括号；</li>
 *   <li>最终包裹为 {@code $...$}；已带 $...$、$$...$$、\(...\)、\[...\] 的内容原样保留。</li>
 * </ul>
 *
 * <p>注意：仅适用于纯文本字段（题干、解析）。选项是 JSON 字符串，且 JSON 引号属于 ASCII，
 * 直接处理会把引号吞进公式，需要在解析成结构后再逐项归一化。</p>
 */
public final class LatexTextNormalizer {

    private LatexTextNormalizer() {
    }

    /** 独立成词的数学函数名：命中即视为公式片段起点 */
    private static final List<String> FUNCTION_NAMES = List.of(
            "lim", "sin", "cos", "tan", "cot", "sec", "csc",
            "arcsin", "arccos", "arctan", "ln", "log", "exp");

    /** Unicode 数学符号 → LaTeX 命令（模型常直接写 →、≤、∞ 等） */
    private static final Map<Character, String> UNICODE_MATH = new LinkedHashMap<>();

    static {
        UNICODE_MATH.put('→', "\\to ");
        UNICODE_MATH.put('←', "\\leftarrow ");
        UNICODE_MATH.put('∞', "\\infty ");
        UNICODE_MATH.put('≤', "\\le ");
        UNICODE_MATH.put('≥', "\\ge ");
        UNICODE_MATH.put('≠', "\\ne ");
        UNICODE_MATH.put('×', "\\times ");
        UNICODE_MATH.put('÷', "\\div ");
        UNICODE_MATH.put('±', "\\pm ");
        UNICODE_MATH.put('⋅', "\\cdot ");
        UNICODE_MATH.put('·', "\\cdot ");
        UNICODE_MATH.put('π', "\\pi ");
        UNICODE_MATH.put('∈', "\\in ");
        UNICODE_MATH.put('∑', "\\sum ");
        UNICODE_MATH.put('∫', "\\int ");
    }

    /** 多位数字上下标补花括号：x^12 → x^{12}（单字符在 LaTeX 中本就合法，保持原样） */
    private static final Pattern MULTI_CHAR_SCRIPT = Pattern.compile("([\\^_])(\\d{2,})");

    public static String wrapBareMath(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        int length = text.length();
        StringBuilder sb = new StringBuilder(length + 16);
        boolean inMath = false;
        boolean inBlockMath = false;
        int i = 0;
        while (i < length) {
            char c = text.charAt(i);

            // —— 已带定界符的内容原样保留 ——
            if (!inMath && !inBlockMath && text.startsWith("$$", i)) {
                sb.append("$$");
                i += 2;
                inBlockMath = true;
                continue;
            }
            if (inBlockMath && text.startsWith("$$", i)) {
                sb.append("$$");
                i += 2;
                inBlockMath = false;
                continue;
            }
            if (!inMath && !inBlockMath && text.startsWith("\\[", i)) {
                sb.append("\\[");
                i += 2;
                inBlockMath = true;
                continue;
            }
            if (inBlockMath && text.startsWith("\\]", i)) {
                sb.append("\\]");
                i += 2;
                inBlockMath = false;
                continue;
            }
            if (!inMath && !inBlockMath && text.startsWith("\\(", i)) {
                sb.append("\\(");
                i += 2;
                inMath = true;
                continue;
            }
            if (inMath && text.startsWith("\\)", i)) {
                sb.append("\\)");
                i += 2;
                inMath = false;
                continue;
            }
            if (!inMath && !inBlockMath && c == '$') {
                sb.append('$');
                i++;
                inMath = true;
                continue;
            }
            if (inMath && c == '$') {
                sb.append('$');
                i++;
                inMath = false;
                continue;
            }
            if (inMath || inBlockMath) {
                sb.append(c);
                i++;
                continue;
            }

            // —— 裸文本区：识别公式片段 ——
            if (isSegmentStart(text, i)) {
                int end = expandMathSegment(text, i);
                int contentEnd = end;
                while (contentEnd > i && Character.isWhitespace(text.charAt(contentEnd - 1))) {
                    contentEnd--;
                }
                if (contentEnd == i) {
                    sb.append(c);
                    i++;
                    continue;
                }
                sb.append('$').append(toLatex(text.substring(i, contentEnd))).append('$');
                sb.append(text, contentEnd, end);
                i = end;
                continue;
            }

            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** 片段起点：\命令 或 独立成词的函数名 */
    private static boolean isSegmentStart(String text, int index) {
        char c = text.charAt(index);
        if (c == '\\') {
            return index + 1 < text.length() && isAsciiLetter(text.charAt(index + 1));
        }
        if (!isAsciiLetter(c)) {
            return false;
        }
        if (index > 0 && isAsciiLetter(text.charAt(index - 1))) {
            return false;
        }
        for (String name : FUNCTION_NAMES) {
            int end = index + name.length();
            if (!text.regionMatches(true, index, name, 0, name.length())) {
                continue;
            }
            // 名称后不能再接字母，避免把 cost / logs 之类普通单词当函数
            return end >= text.length() || !isAsciiLetter(text.charAt(end));
        }
        return false;
    }

    /** 片段内容：连续 ASCII 与常见 Unicode 数学符号，遇中文/全角标点/换行/美元符终止 */
    private static int expandMathSegment(String text, int start) {
        int i = start;
        while (i < text.length() && isMathSegmentChar(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean isMathSegmentChar(char c) {
        if (c == '$' || c == '\n' || c == '\r') {
            return false;
        }
        if (UNICODE_MATH.containsKey(c)) {
            return true;
        }
        if (Character.isWhitespace(c)) {
            return c == ' ' || c == '\t';
        }
        return c < 0x80;
    }

    /** 片段内的轻量 LaTeX 化：函数名补反斜杠、Unicode 符号转命令、多位脚本加花括号 */
    private static String toLatex(String segment) {
        String result = segment;
        // 函数名补反斜杠：已带反斜杠的（\lim）不会被重复加前缀
        for (String name : FUNCTION_NAMES) {
            result = Pattern
                    .compile("(?<![\\\\A-Za-z])(" + name + ")(?![A-Za-z])", Pattern.CASE_INSENSITIVE)
                    .matcher(result)
                    .replaceAll("\\\\" + name);
        }
        for (Map.Entry<Character, String> entry : UNICODE_MATH.entrySet()) {
            result = result.replace(entry.getKey().toString(), entry.getValue());
        }
        Matcher matcher = MULTI_CHAR_SCRIPT.matcher(result);
        StringBuilder normalized = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(normalized, matcher.group(1) + "{" + matcher.group(2) + "}");
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
