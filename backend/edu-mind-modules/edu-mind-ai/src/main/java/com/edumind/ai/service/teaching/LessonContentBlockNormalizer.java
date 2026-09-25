package com.edumind.ai.service.teaching;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 课节块式正文（讲义）落库前的结构规范化。
 *
 * <p>模型拿到课节标题后，常把标题当作正文的第一个 Markdown 标题再写一遍
 * （{@code ##1.2 洛必达法则求未定式极限专项突破本课节围绕两条主线展开：…}）。
 * 而课节工作台与学习页已经用课节标题作为页面标题展示，于是同一屏出现两个
 * 「1.2 xxx专项突破」，教师会以为内容重复。</p>
 *
 * <p>职责边界：本类只处理「标题回声」这一确定性缺陷（比对依据是真实课节标题），
 * 不做模糊排版猜测。模型偶发的「标题与正文写在同一行」等格式问题，
 * 由前端渲染层统一容错（见 {@code utils/format/lesson-markdown.ts}），避免同一套启发式
 * 规则在前后端各写一份、日后各自漂移。</p>
 */
@Component
public class LessonContentBlockNormalizer {

    /** 标题回声的最短长度：过短（如「极限」）极易误伤正文，直接跳过 */
    private static final int MIN_TITLE_LENGTH = 4;

    /**
     * 规范化课节正文 JSON：剥离各块开头与课节标题等价的 Markdown 标题回声。
     *
     * @param contentJson 模型输出的块式正文 JSON
     * @param lessonTitle 课节标题（页面标题，正文中不应重复出现）
     * @return 规范化后的 JSON；无法解析或无需改动时原样返回
     */
    public String normalize(String contentJson, String lessonTitle) {
        if (!StringUtils.hasText(contentJson) || !StringUtils.hasText(lessonTitle)) {
            return contentJson;
        }
        try {
            JSONObject root = JSON.parseObject(contentJson);
            if (root == null) {
                return contentJson;
            }
            JSONArray blocks = root.getJSONArray("blocks");
            if (blocks == null || blocks.isEmpty()) {
                return contentJson;
            }
            boolean changed = false;
            for (int i = 0; i < blocks.size(); i++) {
                JSONObject block = blocks.getJSONObject(i);
                if (block == null) {
                    continue;
                }
                String body = block.getString("body");
                String normalized = stripTitleEcho(body, lessonTitle);
                if (normalized != null && !normalized.equals(body)) {
                    block.put("body", normalized);
                    changed = true;
                }
            }
            return changed ? JSON.toJSONString(root) : contentJson;
        } catch (Exception ex) {
            // 规范化失败不能影响备课：原样落库，由前端渲染层兜底
            return contentJson;
        }
    }

    /**
     * 剥离正文开头的课节标题回声。
     *
     * <p>比较时折叠空白与省略号：模型回写的标题常丢空格、丢编号分隔，
     * 折叠后才能与真实标题对齐。</p>
     *
     * @return 去掉回声后的正文（可能为空字符串）；正文开头没有标题回声时返回 {@code null}
     */
    String stripTitleEcho(String body, String lessonTitle) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        String trimmed = body.stripLeading();
        if (!trimmed.startsWith("#")) {
            return null;
        }
        int cursor = 0;
        while (cursor < trimmed.length() && trimmed.charAt(cursor) == '#') {
            cursor++;
        }
        while (cursor < trimmed.length() && Character.isWhitespace(trimmed.charAt(cursor))) {
            cursor++;
        }
        String line = trimmed.substring(cursor);
        String foldedTitle = fold(lessonTitle);
        if (foldedTitle.length() < MIN_TITLE_LENGTH || !fold(line).startsWith(foldedTitle)) {
            return null;
        }
        int cut = consumeFolded(line, foldedTitle.length());
        if (cut < 0) {
            return null;
        }
        // 标题回声之后的同行正文（模型常把标题与正文写在同一行）+ 标题行之后的其余内容
        String inlineRest = line.substring(cut).strip();
        String tail = trimmed.substring(cursor + line.length());
        String result = (inlineRest + tail).stripLeading();
        return result.strip().isEmpty() ? "" : result;
    }

    /** 折叠空白与省略号，用于标题比对 */
    private static String fold(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", "").replaceAll("[…⋯]|\\.{3}", "");
    }

    /** 在原文中按「折叠后的字符数」定位切割点，跳过空白与省略号 */
    private static int consumeFolded(String raw, int foldedLength) {
        int count = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isWhitespace(c) || c == '…' || c == '⋯') {
                continue;
            }
            if (++count == foldedLength) {
                return i + 1;
            }
        }
        return -1;
    }
}
