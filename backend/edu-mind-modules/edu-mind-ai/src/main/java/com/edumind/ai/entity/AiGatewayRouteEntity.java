package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_gateway_route")
public class AiGatewayRouteEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String scene;
    private String primaryModelKey;
    private String fallbackModelKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
