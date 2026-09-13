package com.edumind.ai.service.memory.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 检索召回的长期记忆上下文片段模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryContextBlock implements Serializable {
    private Long id;
    private String summary;
    private String fullContent;
    private String memoryType;
    private String sensitivityLevel;
    private Double score;
    private Boolean encrypted;
}
