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
            String sectionHeading = extractHeading(section);
            if (buffer.length() > 0 && buffer.length() + section.length() + 2 > chunkSize) {
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
                index = emitPiece(chunks, piece, bufferHeading, index, charOffset, charsPerPage);
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

    private List<String> splitIntoSections(String normalized) {
        boolean hasChapterHeadings = normalized.startsWith("## ")
                || normalized.contains("\n## ");
        String delimiter = hasChapterHeadings ? "(?m)(?=^## \\s)" : "(?m)(?=^#{2,6}\\s)";
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
        String segment = buffer.toString().trim();
        buffer.setLength(0);
        if (segment.isEmpty()) {
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
            int breakAt = findBreakPoint(text, 0, end);
            if (breakAt > 0) {
                end = breakAt;
            }
        }
        String piece = text.substring(0, end).trim();
        int rawNext = Math.max(end - overlap, 0);
        int nextStart = alignOverlapStart(text, rawNext, overlap);
        buffer.delete(0, nextStart);
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

    private String extractHeading(String section) {
        if (section == null || section.isBlank()) {
            return null;
        }
        String firstLine = section.lines().findFirst().orElse("").trim();
        if (firstLine.startsWith("#")) {
            return firstLine.replaceAll("^#+\\s*", "").trim();
        }
        return null;
    }

    private int findBreakPoint(String text, int start, int end) {
        int subHeading = text.lastIndexOf("\n### ", end);
        if (subHeading > start + 80) {
            return subHeading + 1;
        }
        int chapter = text.lastIndexOf("\n## ", end);
        if (chapter > start + 80) {
            return chapter + 1;
        }
        int listItem = text.lastIndexOf("\n- ", end);
        if (listItem > start + 120) {
            return listItem + 1;
        }
        int blankLine = text.lastIndexOf("\n\n", end);
        if (blankLine > start + 120) {
            return blankLine + 2;
        }
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '；' || c == ';' || c == '，' || c == ','
                    || c == '.' || c == '！' || c == '？') {
                return i + 1;
            }
        }
        return end;
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
