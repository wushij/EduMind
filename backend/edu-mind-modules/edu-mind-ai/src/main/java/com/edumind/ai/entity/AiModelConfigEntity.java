package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_model_config")
public class AiModelConfigEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelKey;
    private String provider;
    private Boolean enabled;
    private Integer priority;
    private String fallbackModelKey;
    private Integer maxTokens;
    private BigDecimal temperature;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
