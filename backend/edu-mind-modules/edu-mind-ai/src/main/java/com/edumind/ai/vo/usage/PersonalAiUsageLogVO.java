package com.edumind.ai.vo.usage;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PersonalAiUsageLogVO {

    private Long id;
    private String scene;
    private String sceneLabel;
    private String model;
    private Integer totalTokens;
    private LocalDateTime createTime;
}
