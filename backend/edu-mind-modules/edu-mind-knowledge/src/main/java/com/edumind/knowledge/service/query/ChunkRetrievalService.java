package com.edumind.knowledge.service.query;

import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.vo.knowledge.ChunkKeywordSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChunkRetrievalService {

    private static final int MAX_QUERY_RUNES = 200;
    private static final Pattern ENGLISH_TOKEN = Pattern.compile("[A-Za-z][A-Za-z0-9_+-]{1,}");

    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;

    public ChunkKeywordSearchVO searchKeywords(Long knowledgeBaseId, Long documentId, String query, int limit) {
        String capped = capQuery(query);
        if (!StringUtils.hasText(capped) || knowledgeBaseId == null || limit <= 0) {
            return ChunkKeywordSearchVO.builder()
                    .phraseRankedChunkIds(List.of())
                    .tokenRankedChunkIds(List.of())
                    .techTermRankedChunkIds(List.of())
                    .build();
        }
        int branchLimit = Math.max(limit, 20);
        List<Long> phrase = knowledgeDocumentChunkDao.findIdsByPhraseMatch(
                knowledgeBaseId, documentId, capped, branchLimit);
        List<String> tokens = splitSearchTokens(capped);
        List<Long> tokenHits = tokens.isEmpty()
                ? List.of()
                : knowledgeDocumentChunkDao.findIdsByTokenOrMatch(
                knowledgeBaseId, documentId, tokens, branchLimit);
        List<String> english = extractEnglishTokens(capped);
        List<Long> techHits = english.size() >= 2
                ? knowledgeDocumentChunkDao.findIdsByTechTermsAndMatch(
                knowledgeBaseId, documentId, english, branchLimit)
                : List.of();
        return ChunkKeywordSearchVO.builder()
                .phraseRankedChunkIds(phrase)
                .tokenRankedChunkIds(tokenHits)
                .techTermRankedChunkIds(techHits)
                .build();
    }

    private String capQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }
        String trimmed = query.trim();
        if (trimmed.length() <= MAX_QUERY_RUNES) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_QUERY_RUNES);
    }

    private List<String> splitSearchTokens(String query) {
        List<String> tokens = new ArrayList<>();
        for (String part : query.split("\\s+")) {
            if (part.length() >= 2) {
                tokens.add(part);
            }
        }
        if (tokens.isEmpty() && query.length() >= 2) {
            tokens.add(query);
        }
        return tokens;
    }

    private List<String> extractEnglishTokens(String query) {
        List<String> tokens = new ArrayList<>();
        var matcher = ENGLISH_TOKEN.matcher(query);
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }
}
