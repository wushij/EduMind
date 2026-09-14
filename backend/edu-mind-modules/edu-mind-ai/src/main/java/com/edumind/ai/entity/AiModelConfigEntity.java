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
    /** 配置唯一标识（管理端业务键，对齐 goblog name） */
    private String configName;
    /** Gateway 路由键，默认与 configName 一致 */
    private String modelKey;
    private String provider;
    /** chat | embedding */
    private String configType;
    /** 上游模型型号 */
    private String modelName;
    private String baseUrl;
    private String apiKeyCipher;
    /** KMS 密钥版本号 (>= 1) */
    private Integer keyVersion;
    private Boolean enabled;
    private Integer priority;
    private String fallbackModelKey;
    private Integer maxTokens;
    private BigDecimal temperature;
    private Boolean isDefault;
    /** low | medium | high | max */
    private String reasoningEffort;
    private Integer dimension;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
