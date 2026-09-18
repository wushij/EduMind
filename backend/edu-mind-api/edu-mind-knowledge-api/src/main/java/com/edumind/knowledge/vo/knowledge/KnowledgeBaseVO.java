package com.edumind.knowledge.vo.knowledge;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseVO {
    private Long id;
    private String name;
    private String description;
    private Long courseId;
    private String courseName;
    private String category;
    private Integer docCount;
    private Integer chunkCount;
    private String indexStatus;
    private Integer vectorCount;
    private String embeddingModel;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
