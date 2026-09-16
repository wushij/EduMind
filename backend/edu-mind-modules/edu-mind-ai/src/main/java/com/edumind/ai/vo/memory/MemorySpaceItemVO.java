package com.edumind.ai.vo.memory;

import lombok.Data;

@Data
public class MemorySpaceItemVO {
    private Long namespaceId;
    private Long courseId;
    private String courseTitle;
    private String scope;
    private Boolean consentGranted;
    private Integer retentionDays;
    private Integer itemCount;
}
