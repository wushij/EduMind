package com.edumind.ai.vo;

import lombok.Data;

@Data
public class AiToolVO {
    private String id;
    private String name;
    private String description;
    private String category;
    private String icon;
    private String route;
    private String tags;
    private Boolean isRecommended;
    private Integer useCount;
}
