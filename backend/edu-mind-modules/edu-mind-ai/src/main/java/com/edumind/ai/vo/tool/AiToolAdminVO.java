package com.edumind.ai.vo.tool;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiToolAdminVO {
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
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
