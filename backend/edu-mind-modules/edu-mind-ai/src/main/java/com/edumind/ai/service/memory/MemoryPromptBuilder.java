package com.edumind.ai.service.memory;

import com.edumind.ai.prompt.AiPromptConstants;
import com.edumind.ai.service.memory.retrieval.MemoryContextBlock;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 长期记忆 Prompt 组装与 SSE Payload 构建器 (单一职责，防止敏感信息与明文泄露)
 */
@Component
public class MemoryPromptBuilder {

    /**
     * 将记忆上下文块格式化为注入 System Prompt 的文本段
     * HIGH_RISK 条目使用脱敏 summary，如果存在已授权解密的 fullContent 且非 HIGH_RISK 可供 prompt 辅助推演
     */
    public String toPromptBlock(List<MemoryContextBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        String lines = blocks.stream()
                .map(b -> {
                    String type = StringUtils.hasText(b.getMemoryType()) ? b.getMemoryType() : "PREFERENCE";
                    String content = b.getSummary();
                    if (!"HIGH_RISK".equalsIgnoreCase(b.getSensitivityLevel()) && StringUtils.hasText(b.getFullContent())) {
                        content = b.getSummary() + " (详情: " + b.getFullContent() + ")";
                    }
                    return String.format("- [%s] %s", type, content);
                })
                .collect(Collectors.joining("\n"));

        return String.format(AiPromptConstants.MEMORY_CONTEXT_BLOCK_TEMPLATE, lines);
    }

    /**
     * 构建 SSE memory 事件传输的 payload (严格剔除 fullContent 明文以防前端/信道泄露)
     */
    public List<Map<String, Object>> toSsePayload(List<MemoryContextBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return Collections.emptyList();
        }
        return blocks.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("summary", b.getSummary() != null ? b.getSummary() : "");
            map.put("memoryType", b.getMemoryType() != null ? b.getMemoryType() : "PREFERENCE");
            map.put("score", b.getScore() != null ? b.getScore() : 0.95);
            map.put("encrypted", Boolean.TRUE.equals(b.getEncrypted()));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 将长期记忆块拼接到基础 System Prompt 末尾 (保持 RAG 知识体系与角色定义优先，记忆个性化补充)
     */
    public String buildSystemPromptWithMemory(String baseSystemPrompt, List<MemoryContextBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return baseSystemPrompt != null ? baseSystemPrompt : "";
        }
        String promptBlock = toPromptBlock(blocks);
        if (!StringUtils.hasText(baseSystemPrompt)) {
            return promptBlock.trim();
        }
        return baseSystemPrompt + "\n" + promptBlock;
    }
}
