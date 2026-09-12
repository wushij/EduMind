package com.edumind.ai.vo.gateway;

import lombok.Data;

@Data
public class GatewayRouteVO {
    private String scene;
    private String primaryModelKey;
    private String fallbackModelKey;
}
