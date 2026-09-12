package com.edumind.ai.dto.tool;

import lombok.Data;

@Data
public class SummaryDTO {
    private Long courseId;
    private Long documentId;
    private String content;
    private Boolean useRag;
}
