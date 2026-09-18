package com.edumind.common.markdown;

/**
 * 入库切片前规范化 Markdown：修复课节编辑器常见的「标题与正文粘连」「## 后无空格」等问题。
 */
public final class MarkdownIndexNormalizer {

    private MarkdownIndexNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return text != null ? text : "";
        }
        String s = text.replace("\r\n", "\n").trim();
        // 「正文###小标题」：在 # 标题前断行
        s = s.replaceAll("([^\\n#])(#{2,6})", "$1\n\n$2");
        // 行首 ##一、 / ###2.1 → ## 一、 / ### 2.1
        s = s.replaceAll("(?m)^(#{1,6})([^ #\\n])", "$1 $2");
        // 2.1从 / 2.2四个 → 2.1 从 / 2.2 四个
        s = s.replaceAll("(?m)^(#{1,6}\\s*)(\\d+(?:\\.\\d+)*)([^\\s\\d#.\\n])", "$1$2 $3");
        // 「说起」与后文「在…」粘连
        s = s.replaceAll("(说起)(在\\s+[\\u4e00-\\u9fa5])", "$1\n\n$2");
        // 「## 一、…：从」与「一段…说起」合并为完整标题（支持换行或同行粘连）
        s = s.replaceAll(
                "(?m)^(#{2,6}\\s+[^\\n]+：从)\\s*\\n+\\s*(一段[^\\n]+?说起)(?=\\s*在)",
                "$1$2\n\n");
        s = s.replaceAll(
                "(?m)^(#{2,6}\\s+[^\\n]+：从)(一段[^\\n]+?说起)(?=\\s*在)",
                "$1$2\n\n");
        // 标题行与正文起笔粘连（常见讲义排版）
        // 勿拆开「：从一段…」完整标题（如课节导入）
        s = s.replaceAll("(?m)^(#{2,6}\\s+[^\\n]{4,120}?)(?<!从)(一段)", "$1\n\n$2");
        s = s.replaceAll("(?m)^(#{2,6}\\s+[^\\n]{4,120}?)(可以概括)", "$1\n\n$2");
        s = s.replaceAll("(?m)^(#{2,6}\\s+[^\\n]{4,120}?)(以下链路)", "$1\n\n$2");
        // 「定义- JDK」类标题行内列表
        s = s.replaceAll("(?m)^(#{2,6}\\s+[^\\n]+?)(\\s*[-–—]\\s*(?:JDK|JRE|JVM|字节码))", "$1\n\n$2");
        s = s.replaceAll("(?m)^(#{2,6}\\s+[^\\n]+?)(\\s*[-–—]\\s*)$", "$1\n\n");
        // 标题行末冒号后正文过长
        s = s.replaceAll("(?m)^(#{2,6}\\s+[^\\n]{0,120}?)([：:][^\\n#]{20,})$", "$1\n\n$2");
        // 列表项「-字节码」→「- 字节码」
        s = s.replaceAll("(?m)^(-\\s*)([^\\s\\-*])", "$1$2");
        // 「### 4.2 职责对比|表格」：标题与 Markdown 表格分行
        s = s.replaceAll("(?m)^(#{2,6}\\s+[^\\n|]+)(\\|)", "$1\n\n$2");
        // ```javapublic → ```java + 换行（避免代码块粘连导致误切分）
        s = s.replaceAll("```javapublic", "```java\npublic");
        s = s.replaceAll("```java(?=public\\s)", "```java\n");
        return s.trim();
    }
}
