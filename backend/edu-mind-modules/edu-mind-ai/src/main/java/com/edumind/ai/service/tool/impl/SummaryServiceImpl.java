package com.edumind.ai.service.tool.impl;

import com.edumind.ai.dto.tool.SummaryDTO;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.service.tool.SummaryService;
import com.edumind.common.exception.BusinessException;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final LlmClient llmClient;
    private final KnowledgeQueryApi knowledgeQueryApi;

    @Override
    public String summarize(SummaryDTO dto) {
        String source = resolveSource(dto);
        if (!StringUtils.hasText(source)) {
            throw new BusinessException("缺少可总结的文本内容");
        }
        return llmClient.chat(
                "你是教学总结助手，请输出章节要点、易错清单与复习精要，使用 Markdown。",
                "请总结以下内容：\n\n" + source
        );
    }

    private String resolveSource(SummaryDTO dto) {
        if (StringUtils.hasText(dto.getContent())) {
            return dto.getContent();
        }
        if (dto.getDocumentId() != null) {
            String text = knowledgeQueryApi.getDocumentText(dto.getDocumentId());
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return "";
    }
}
