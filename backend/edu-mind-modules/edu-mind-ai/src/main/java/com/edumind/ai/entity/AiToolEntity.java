package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_tool")
public class AiToolEntity implements Serializable {
    @TableId
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
    private Integer isRecommended;
    private Integer isHot;
    private Integer useCount;
    private Integer status;
    private LocalDateTime createTime;
}
