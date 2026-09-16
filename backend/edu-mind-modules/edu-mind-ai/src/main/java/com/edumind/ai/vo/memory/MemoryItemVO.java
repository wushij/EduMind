package com.edumind.ai.vo.memory;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemoryItemVO {
    private Long id;
    private Long namespaceId;
    private String memoryType;
    private String summary;
    private String fullContent;
    private String sensitivityLevel;
    private String vectorRef;
    private Double confidenceScore;
    private Boolean encrypted;
    private Integer keyVersion;
    private Integer accessCount;
    private String sourceChannel;
    private String sourceRef;
    private String reasoning;
    private Boolean isNewlyCreated;
    private LocalDateTime createTime;
}
