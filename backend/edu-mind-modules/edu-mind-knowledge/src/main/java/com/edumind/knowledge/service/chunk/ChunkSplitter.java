package com.edumind.knowledge.service.chunk;

import com.edumind.common.markdown.MarkdownIndexNormalizer;
import com.edumind.knowledge.config.ChunkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChunkSplitter {

    private final ChunkProperties chunkProperties;

    public List<SplitChunk> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = MarkdownIndexNormalizer.normalize(text);
        int chunkSize = Math.max(chunkProperties.getSize(), 256);
        int overlap = Math.min(chunkProperties.getOverlap(), chunkSize / 2);
        int charsPerPage = Math.max(chunkProperties.getCharsPerPage(), 1000);

        List<String> sections = mergeHeadingOnlySections(splitIntoSections(normalized));
        List<SplitChunk> chunks = new ArrayList<>();
        int index = 0;
        int charOffset = 0;

        StringBuilder buffer = new StringBuilder();
        String bufferHeading = null;

        for (String section : sections) {
            // 如果 buffer 里残留的内容全是分割线或空白，直接丢弃，严禁作为切片落库
            if (buffer.length() > 0 && isMeaninglessContent(buffer.toString())) {
                buffer.setLength(0);
                bufferHeading = null;
            }

            String sectionHeading = extractHeading(section);

            // 核心大章节隔离准则（Major Chapter Isolation）：
            // 若当前 section 是顶级大章节（如 ## 三、或中文大章序号），且 buffer 中残留着上一章节的末尾内容，
            // 必须强制先将上一章节收尾落库（flushChunk），绝对禁止跨大章节强行拼接！
            if (buffer.length() > 0 && isMajorChapterBoundary(section)) {
                int emitted = buffer.length();
                index = flushChunk(chunks, buffer, bufferHeading, index, charOffset, charsPerPage);
                charOffset += emitted;
                bufferHeading = sectionHeading;
                buffer.append(section);
            } else if (buffer.length() > 0 && buffer.length() + section.length() + 2 > chunkSize) {
                int emitted = buffer.length();
                index = flushChunk(chunks, buffer, bufferHeading, index, charOffset, charsPerPage);
                charOffset += emitted;
                bufferHeading = sectionHeading;
                buffer.append(section);
            } else {
                if (buffer.length() > 0) {
                    buffer.append("\n\n");
                }
                if (buffer.length() == 0) {
                    bufferHeading = sectionHeading;
                }
                buffer.append(section);
            }
            while (buffer.length() > chunkSize) {
                String piece = takeFirstChunk(buffer, chunkSize, overlap);
                if (!isMeaninglessContent(piece)) {
                    index = emitPiece(chunks, piece, bufferHeading, index, charOffset, charsPerPage);
                }
                charOffset += piece.length();
                bufferHeading = extractHeading(buffer.toString());
            }
        }
        if (buffer.length() > 0) {
            flushChunk(chunks, buffer, bufferHeading, index, charOffset, charsPerPage);
        }
        return coalesceHeadingOnlyChunks(chunks);
    }

    /** 将「仅一行标题」的碎片并入下一块，避免序号与正文错位 */
    private List<SplitChunk> coalesceHeadingOnlyChunks(List<SplitChunk> chunks) {
        if (chunks.size() < 2) {
            return chunks;
        }
        List<SplitChunk> out = new ArrayList<>();
        SplitChunk pending = null;
        for (SplitChunk chunk : chunks) {
            if (isHeadingOnlySection(chunk.getContent())) {
                if (pending != null) {
                    out.add(pending);
                }
                pending = chunk;
                continue;
            }
            if (pending != null) {
                pending.setContent(pending.getContent() + "\n\n" + chunk.getContent());
                pending.setCharCount(pending.getContent().length());
                pending.setTokenEstimate(Math.max(1, pending.getContent().length() / 4));
                pending.setHeading(extractHeading(pending.getContent()));
                out.add(pending);
                pending = null;
            } else {
                out.add(chunk);
            }
        }
        if (pending != null) {
            if (!out.isEmpty()) {
                SplitChunk last = out.get(out.size() - 1);
                last.setContent(last.getContent() + "\n\n" + pending.getContent());
                last.setCharCount(last.getContent().length());
                last.setTokenEstimate(Math.max(1, last.getContent().length() / 4));
            } else {
                out.add(pending);
            }
        }
        out.removeIf(c -> isMeaninglessContent(c.getContent()));
        for (int i = 0; i < out.size(); i++) {
            out.get(i).setChunkIndex(i);
        }
        return out;
    }

    /** 避免「### 4.1 标题」单独成块、列表落在下一块 */
    private List<String> mergeHeadingOnlySections(List<String> sections) {
        if (sections.size() < 2) {
            return sections;
        }
        List<String> merged = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            String sec = sections.get(i);
            if (isHeadingOnlySection(sec) && i + 1 < sections.size()) {
                merged.add(sec + "\n\n" + sections.get(i + 1));
                i++;
            } else {
                merged.add(sec);
            }
        }
        return merged;
    }

    private boolean isHeadingOnlySection(String section) {
        if (section == null || section.isBlank()) {
            return false;
        }
        String nonEmpty = null;
        int count = 0;
        for (String line : section.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            count++;
            nonEmpty = t;
        }
        return count == 1 && nonEmpty != null && nonEmpty.startsWith("#");
    }

    List<String> splitIntoSectionsForTest(String normalized) {
        return splitIntoSections(normalized);
    }

    private List<String> splitIntoSections(String normalized) {
        boolean hasChapterHeadings = normalized.startsWith("## ")
                || normalized.contains("\n## ")
                || normalized.startsWith("##\t")
                || normalized.contains("\n##\t");
        String delimiter = hasChapterHeadings ? "(?m)(?=^##\\s*)" : "(?m)(?=^#{2,6}\\s*)";
        String[] parts = normalized.split(delimiter);
        List<String> sections = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sections.add(trimmed);
            }
        }
        if (sections.isEmpty()) {
            sections.add(normalized);
        }
        return sections;
    }

    private int flushChunk(List<SplitChunk> chunks, StringBuilder buffer, String heading, int index,
                           int charOffset, int charsPerPage) {
        String segment = stripDividers(buffer.toString());
        buffer.setLength(0);
        if (isMeaninglessContent(segment)) {
            return index;
        }
        return emitPiece(chunks, segment, heading != null ? heading : extractHeading(segment), index, charOffset,
                charsPerPage);
    }

    private int emitPiece(List<SplitChunk> chunks, String segment, String heading, int index, int charOffset,
                          int charsPerPage) {
        SplitChunk chunk = new SplitChunk();
        chunk.setChunkIndex(index++);
        chunk.setContent(segment);
        chunk.setHeading(heading);
        chunk.setCharCount(segment.length());
        chunk.setTokenEstimate(Math.max(1, segment.length() / 4));
        chunk.setPageNo(1 + charOffset / charsPerPage);
        chunks.add(chunk);
        return index;
    }

    private String takeFirstChunk(StringBuilder buffer, int chunkSize, int overlap) {
        String text = buffer.toString();
        int end = Math.min(chunkSize, text.length());

        if (end < text.length()) {
            int minBreak = Math.max(overlap + 16, 144);
            // 围栏代码块（```）保护：严禁盲目在代码块/Mermaid/ASCII图表内部截断
            if (isInsideCodeBlock(text, end)) {
                int closingFence = text.indexOf("```", end);
                // 完整图表/代码块贪婪延伸保护：如果代码块在适度延伸后（如 1250~1300 字内）即可完整闭合，
                // 优先完整保留整张架构图或代码块，绝不切成残片！
                int extendLimit = Math.max(chunkSize + 500, (int) (chunkSize * 1.6));
                if (closingFence != -1 && closingFence + 3 <= extendLimit && closingFence + 3 <= text.length()) {
                    int nextLine = text.indexOf("\n", closingFence + 3);
                    end = nextLine != -1 ? nextLine + 1 : text.length();
                    // 如果代码块后面紧跟着的只剩分割线（---、***）或空白，一并消耗掉，避免孤立的 --- 残留在 buffer 中
                    String remainder = text.substring(end).trim();
                    if (isMeaninglessContent(remainder)) {
                        end = text.length();
                    }
                } else {
                    // 如果代码块不是从开头就铺满的，优先在代码块开始前截断，让代码块完整挪到下一块开始
                    int openingFence = text.lastIndexOf("```", end);
                    int breakBefore = openingFence > 0 ? text.lastIndexOf("\n", openingFence - 1) : -1;
                    if (breakBefore >= minBreak) {
                        end = breakBefore + 1;
                    } else {
                        // 代码块自身超长（例如上千行的长代码），必须在代码块内部换行符 '\n' 处截断
                        int codeLineBreak = text.lastIndexOf("\n", end);
                        if (codeLineBreak >= minBreak) {
                            end = codeLineBreak + 1;
                        }
                    }
                }
            } else {
                int breakAt = findBreakPoint(text, minBreak, end);
                if (breakAt >= minBreak) {
                    end = breakAt;
                }
            }
        }

        String piece = stripDividers(text.substring(0, end));

        // 围栏语法补齐：如果切片在代码块内部结束，为当前切片闭合 ```，保持 Markdown 渲染合法
        boolean leftUnclosed = isInsideCodeBlock(text, end);
        String unclosedLang = leftUnclosed ? getUnclosedCodeBlockLanguage(text, end) : null;
        if (leftUnclosed) {
            piece = piece + "\n```";
        }

        int nextStart;
        if (leftUnclosed) {
            // 核心规则：切片如果在代码块内部截断，下一块必须严格从截断处（即换行符后）无缝接续，
            // 绝对禁止在代码/ASCII图表内部执行 overlap 倒退，否则会出现「/ Old Gen) |」这种断裂残行！
            nextStart = end;
        } else {
            int rawNext = Math.max(end - overlap, 0);
            nextStart = alignOverlapStart(text, rawNext, overlap);
            // 如果当前切片已经闭合了代码块，overlap 回退绝不能倒退进该代码块内部，避免下一块截取代码尾部残片
            if (isInsideCodeBlock(text, nextStart)) {
                int lastClosing = text.lastIndexOf("```", end);
                if (lastClosing >= nextStart) {
                    int afterFence = text.indexOf("\n", lastClosing + 3);
                    nextStart = afterFence != -1 ? afterFence + 1 : lastClosing + 3;
                }
            }
            // 如果从 nextStart 开始到末尾剩下的全都是无意义分割线或空白，直接全部消费
            if (nextStart < text.length() && isMeaninglessContent(text.substring(nextStart))) {
                nextStart = text.length();
            }
        }

        // 严格保证消费步长：nextStart 必须至少推进 Math.max(1, end - overlap)，杜绝 0 步长死循环
        int minConsume = Math.max(1, end - overlap);
        if (nextStart < minConsume || nextStart > end) {
            nextStart = minConsume;
        }
        buffer.delete(0, nextStart);

        // 如果下一块开头仍处于被切开的代码块内部，为下一块补齐开头的 ```{lang}
        if (buffer.length() > 0 && isInsideCodeBlock(text, nextStart)) {
            String nextLang = getUnclosedCodeBlockLanguage(text, nextStart);
            String reopen = "```" + (nextLang != null ? nextLang : "") + "\n";
            buffer.insert(0, reopen);
        }

        return piece;
    }

    /**
     * overlap 回退不能落在半句话/半个词中间，否则下一块会出现「擎；」类残片。
     */
    private int alignOverlapStart(String text, int rawNext, int overlapWindow) {
        if (rawNext <= 0) {
            return 0;
        }
        int len = text.length();
        if (rawNext >= len) {
            return len;
        }
        int forwardLimit = Math.min(len, rawNext + overlapWindow + 96);
        for (int i = rawNext; i < forwardLimit; i++) {
            if (isChunkStartBoundary(text, i)) {
                return i;
            }
        }
        int backLimit = Math.max(0, rawNext - overlapWindow);
        for (int i = rawNext - 1; i >= backLimit; i--) {
            int after = i + 1;
            if (after < len && isChunkStartBoundary(text, after)) {
                return after;
            }
            char c = text.charAt(i);
            if (c == '。' || c == '；' || c == ';' || c == '！' || c == '？' || c == '\n') {
                return after;
            }
        }
        return rawNext;
    }

    private boolean isChunkStartBoundary(String text, int index) {
        if (index <= 0) {
            return true;
        }
        if (text.startsWith("\n\n", index)) {
            return true;
        }
        if (text.startsWith("\n### ", index) || text.startsWith("\n## ", index)) {
            return true;
        }
        if (text.startsWith("\n- ", index)) {
            return true;
        }
        if (index > 0 && text.charAt(index - 1) == '\n' && text.startsWith("- ", index)) {
            return true;
        }
        return false;
    }

    private boolean isMeaninglessContent(String text) {
        if (text == null) {
            return true;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return true;
        }
        // 仅由横线、下划线、星号构成的 Markdown 分割线（如 ---, ***, ___）
        if (t.matches("^[\\s\\-*_—=]+$")) {
            return true;
        }
        // 去除所有符号、分割线和空白后，若几乎没有实际有效文字字符（长度为0）
        String stripped = t.replaceAll("[\\s\\-*_—=\\.,;:!?'\"`~#|/\\\\，。；：！？“”‘’（）\\[\\]{}()]+", "");
        return stripped.isEmpty();
    }

    private String stripDividers(String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        // 去除开头的横线分割线 --- / *** / ___
        t = t.replaceAll("^(?:\\s*[-*_—=]{3,}\\s*\\n+)+", "");
        // 去除结尾的横线分割线 --- / *** / ___
        t = t.replaceAll("(?:\\n+\\s*[-*_—=]{3,}\\s*)+$", "");
        return t.trim();
    }

    private boolean isMajorChapterBoundary(String section) {
        if (section == null || section.isBlank()) {
            return false;
        }
        for (String line : section.lines().toList()) {
            String t = line.trim();
            if (t.isEmpty() || t.equals("---") || t.equals("***") || t.equals("___")) {
                continue;
            }
            // 1. Markdown 二级大标题 ##（但不包括 ### 三级小节）
            if (t.startsWith("## ") || t.startsWith("##\t") || t.matches("^##\\s*[^#].*")) {
                return true;
            }
            // 2. 中文大章节序号：一、 二、 三、 或 第一章、第二节
            if (t.matches("^[一二三四五六七八九十百]+[、.：:].*") || t.matches("^第[一二三四五六七八九十0-9]+[章节篇集部].*")) {
                return true;
            }
            break;
        }
        return false;
    }

    private String extractHeading(String section) {
        if (section == null || section.isBlank()) {
            return null;
        }
        for (String line : section.lines().toList()) {
            String t = line.trim();
            if (t.isEmpty() || t.equals("---") || t.equals("***") || t.equals("___")) {
                continue;
            }
            if (t.startsWith("#")) {
                return t.replaceAll("^#+\\s*", "").trim();
            }
            if (t.matches("^[一二三四五六七八九十百]+[、.：:].*") || t.matches("^第[一二三四五六七八九十0-9]+[章节篇集部].*")) {
                return t;
            }
            if (t.matches("^\\d+\\.\\d*\\s*.*") && t.length() < 60) {
                return t;
            }
            break;
        }
        return null;
    }

    private int findBreakPoint(String text, int start, int end) {
        int subHeading = text.lastIndexOf("\n### ", end);
        if (subHeading >= start && !isInsideCodeBlock(text, subHeading)) {
            return subHeading + 1;
        }
        int chapter = text.lastIndexOf("\n## ", end);
        if (chapter >= start && !isInsideCodeBlock(text, chapter)) {
            return chapter + 1;
        }
        int listItem = text.lastIndexOf("\n- ", end);
        if (listItem >= start && !isInsideCodeBlock(text, listItem)) {
            return listItem + 1;
        }
        int blankLine = text.lastIndexOf("\n\n", end);
        if (blankLine >= start && !isInsideCodeBlock(text, blankLine)) {
            return blankLine + 2;
        }
        for (int i = end - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '；' || c == ';' || c == '，' || c == ','
                    || c == '.' || c == '！' || c == '？') {
                if (!isInsideCodeBlock(text, i)) {
                    return i + 1;
                }
            }
        }
        return end;
    }

    private boolean isInsideCodeBlock(String text, int index) {
        if (text == null || index <= 0) {
            return false;
        }
        int count = 0;
        int pos = 0;
        int limit = Math.min(index, text.length());
        while ((pos = text.indexOf("```", pos)) != -1 && pos < limit) {
            count++;
            pos += 3;
        }
        return (count % 2) != 0;
    }

    private String getUnclosedCodeBlockLanguage(String text, int index) {
        if (!isInsideCodeBlock(text, index)) {
            return null;
        }
        int lastFence = text.lastIndexOf("```", index);
        if (lastFence == -1) {
            return "";
        }
        int lineEnd = text.indexOf("\n", lastFence);
        if (lineEnd == -1 || lineEnd > index) {
            return "";
        }
        return text.substring(lastFence + 3, lineEnd).trim();
    }

    @lombok.Data
    public static class SplitChunk {
        private int chunkIndex;
        private String content;
        private Integer pageNo;
        private String heading;
        private int charCount;
        private int tokenEstimate;
    }
}
