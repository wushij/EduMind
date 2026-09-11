package com.edumind.knowledge.dto.knowledge;

import lombok.Data;

@Data
public class KnowledgeBaseUpdateDTO {
    private String name;
    private String description;
    private Long courseId;
    private Integer status;
}
