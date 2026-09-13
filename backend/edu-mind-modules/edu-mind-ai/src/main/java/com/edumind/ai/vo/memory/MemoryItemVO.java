package com.edumind.ai.vo.memory;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemoryItemVO {
    private Long id;
    private Long namespaceId;
    private String summary;
    private String sensitivityLevel;
    private String vectorRef;
    private LocalDateTime createTime;
}
