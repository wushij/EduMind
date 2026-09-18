package com.edumind.course.service.lesson;

import com.edumind.common.markdown.MarkdownIndexNormalizer;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 将课节 contentJson 转为 Markdown，并在超长时按提问做段落选取（课程讲义检索，非知识库向量）。
 */
public final class LessonContentMarkdownSupport {

    public static final int DEFAULT_MAX_BODY_CHARS = 28_000;

    private LessonContentMarkdownSupport() {
    }

    public static ParsedLessonText parseContentJson(String contentJson) {
        String objectives = "";
        List<String> markdownParts = new ArrayList<>();
        if (!StringUtils.hasText(contentJson)) {
            return new ParsedLessonText(objectives, "");
        }
        JSONObject doc = JSONObject.parseObject(contentJson);
        JSONArray blocks = doc != null ? doc.getJSONArray("blocks") : null;
        if (blocks == null) {
            return new ParsedLessonText(objectives, "");
        }
        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            if (block == null) {
                continue;
            }
            String type = block.getString("type");
            if ("callout".equals(type) && "objective".equals(block.getString("variant"))) {
                String title = block.getString("title");
                String body = block.getString("body");
                objectives = joinNonBlank(title, body);
                continue;
            }
            if ("heading".equals(type)) {
                String text = block.getString("text");
                if (StringUtils.hasText(text)) {
                    int level = block.getIntValue("level", 2);
                    level = Math.min(Math.max(level, 2), 4);
                    markdownParts.add("#".repeat(level) + " " + text.trim());
                }
                continue;
            }
            if ("markdown".equals(type)) {
                String body = block.getString("body");
                if (StringUtils.hasText(body)) {
                    markdownParts.add(body.trim());
                }
            }
        }
        String body = markdownParts.stream().filter(StringUtils::hasText).collect(Collectors.joining("\n\n"));
        return new ParsedLessonText(objectives.trim(), body);
    }

    public static String selectRelevantBody(String fullBody, String retrievalQuery, int maxChars) {
        if (!StringUtils.hasText(fullBody)) {
            return "";
        }
        if (fullBody.length() <= maxChars) {
            return fullBody;
        }
        List<Section> sections = splitSections(fullBody);
        if (sections.isEmpty()) {
            return truncate(fullBody, maxChars);
        }
        Set<String> queryTokens = tokenize(retrievalQuery);
        sections.sort(Comparator.comparingInt((Section s) -> scoreSection(s, queryTokens)).reversed());

        StringBuilder sb = new StringBuilder();
        sb.append("（以下为本课讲义中与提问最相关的节选；完整讲义共 ").append(fullBody.length()).append(" 字）\n\n");
        int budget = maxChars - sb.length();
        boolean first = true;
        for (Section section : sections) {
            String chunk = section.text;
            if (!first) {
                chunk = "\n\n" + chunk;
            }
            if (chunk.length() > budget) {
                if (budget > 200) {
                    sb.append(truncate(chunk, budget));
                }
                break;
            }
            sb.append(chunk);
            budget -= chunk.length();
            first = false;
            if (budget <= 0) {
                break;
            }
        }
        if (sb.length() < 120 && sections.size() > 0) {
            return truncate(fullBody, maxChars);
        }
        return sb.toString();
    }

    private static List<Section> splitSections(String body) {
        List<Section> sections = new ArrayList<>();
        String[] parts = body.split("(?=\\n#{1,4} )");
        for (String part : parts) {
            String trimmed = part.trim();
            if (StringUtils.hasText(trimmed)) {
                sections.add(new Section(trimmed));
            }
        }
        if (sections.isEmpty()) {
            sections.add(new Section(body.trim()));
        }
        return sections;
    }

    private static int scoreSection(Section section, Set<String> queryTokens) {
        if (queryTokens.isEmpty()) {
            return section.text.length() > 0 ? 1 : 0;
        }
        Set<String> sectionTokens = tokenize(section.text);
        int overlap = 0;
        for (String t : queryTokens) {
            if (sectionTokens.contains(t)) {
                overlap++;
            }
        }
        return overlap * 10 + Math.min(section.text.length() / 500, 5);
    }

    private static Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .filter(s -> s.length() >= 2)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String joinNonBlank(String a, String b) {
        if (!StringUtils.hasText(a)) {
            return b != null ? b.trim() : "";
        }
        if (!StringUtils.hasText(b)) {
            return a.trim();
        }
        return a.trim() + "\n" + b.trim();
    }

    private static String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "…";
    }

    public static String buildIndexMarkdown(String objectivesText, String bodyMarkdown) {
        if (!StringUtils.hasText(objectivesText)) {
            return bodyMarkdown != null ? bodyMarkdown.trim() : "";
        }
        if (!StringUtils.hasText(bodyMarkdown)) {
            return objectivesText.trim();
        }
        String body = bodyMarkdown.trim();
        String objectives = stripRedundantObjectivesHeading(objectivesText.trim());
        String combined = "## 学习目标\n\n" + objectives + "\n\n" + body;
        return normalizeLessonIndexMarkdown(combined);
    }

    /**
     * 课节目标 callout 常自带 title「学习目标」，避免与 buildIndexMarkdown 生成的 ## 学习目标 重复。
     */
    static String stripRedundantObjectivesHeading(String objectives) {
        if (!StringUtils.hasText(objectives)) {
            return objectives != null ? objectives : "";
        }
        String s = objectives.trim();
        if (s.startsWith("学习目标")) {
            s = s.replaceFirst("^学习目标\\s*\\n+", "");
            s = s.replaceFirst("^学习目标\\s*[:：]?\\s*", "");
        }
        return s.trim();
    }

    /**
     * 与知识库 {@code MarkdownIndexNormalizer} 规则对齐，避免课节讲义标题粘连导致 RAG 切片错乱。
     */
    public static String normalizeLessonIndexMarkdown(String markdown) {
        return MarkdownIndexNormalizer.normalize(markdown);
    }

    public static String computeContentHash(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(markdown.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public record ParsedLessonText(String objectivesText, String bodyMarkdown) {
    }

    private record Section(String text) {
    }
}
