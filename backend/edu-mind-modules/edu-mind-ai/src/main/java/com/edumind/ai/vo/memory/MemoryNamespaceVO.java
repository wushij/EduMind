package com.edumind.ai.vo.memory;

import lombok.Data;

import java.util.List;

@Data
public class MemoryNamespaceVO {
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long courseId;
    private String scope;
    private Boolean consentStatus;
    private List<MemoryItemVO> items;
}
