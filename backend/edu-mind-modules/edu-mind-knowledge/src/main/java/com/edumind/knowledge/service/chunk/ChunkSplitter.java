package com.edumind.knowledge.service.chunk;

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
        String normalized = text.replace("\r\n", "\n");
        int chunkSize = Math.max(chunkProperties.getSize(), 256);
        int overlap = Math.min(chunkProperties.getOverlap(), chunkSize / 2);
        int charsPerPage = Math.max(chunkProperties.getCharsPerPage(), 1000);

        List<SplitChunk> chunks = new ArrayList<>();
        String currentHeading = null;
        int index = 0;
        int pos = 0;
        int length = normalized.length();

        while (pos < length) {
            int end = Math.min(pos + chunkSize, length);
            if (end < length) {
                int breakAt = findBreakPoint(normalized, pos, end);
                if (breakAt > pos) {
                    end = breakAt;
                }
            }
            String segment = normalized.substring(pos, end).trim();
            if (!segment.isEmpty()) {
                for (String line : segment.split("\n")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("#")) {
                        currentHeading = trimmed.replaceAll("^#+\\s*", "").trim();
                    }
                }
                SplitChunk chunk = new SplitChunk();
                chunk.setChunkIndex(index++);
                chunk.setContent(segment);
                chunk.setHeading(currentHeading);
                chunk.setCharCount(segment.length());
                chunk.setTokenEstimate(Math.max(1, segment.length() / 4));
                chunk.setPageNo(1 + pos / charsPerPage);
                chunks.add(chunk);
            }
            if (end >= length) {
                break;
            }
            pos = Math.max(end - overlap, pos + 1);
        }
        return chunks;
    }

    private int findBreakPoint(String text, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '.' || c == '！' || c == '？') {
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
