package com.edumind.ai.dto.memory;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemoryConsentDTO {
    private Long courseId;
    private Boolean consent;
    private Boolean consentGranted;
    private Integer retentionDays;

    public boolean isAgreed() {
        if (consentGranted != null) {
            return consentGranted;
        }
        return Boolean.TRUE.equals(consent);
    }
}
