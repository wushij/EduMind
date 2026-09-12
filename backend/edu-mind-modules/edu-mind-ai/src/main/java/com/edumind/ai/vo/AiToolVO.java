package com.edumind.ai.vo;

import lombok.Data;

@Data
public class AiToolVO {
    private String id;
    private String name;
    private String description;
    private String detailedIntro;
    private String category;
    private String icon;
    private String modelId;
    private String route;
    private String executionMode;
    private String tags;
    private Boolean isRecommended;
    private Boolean isHot;
    private Integer useCount;
}
