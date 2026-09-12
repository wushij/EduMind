package com.edumind.ai.service.gateway;

import com.edumind.ai.vo.gateway.AiModelConfigVO;
import com.edumind.ai.vo.gateway.GatewayMetricsVO;
import com.edumind.ai.vo.gateway.GatewayRouteVO;

import java.util.List;

public interface GatewayManageService {

    GatewayMetricsVO getMetrics(String range);

    List<AiModelConfigVO> listModels();

    List<AiModelConfigVO> listEnabledChatModels();

    void updateModel(Long id, AiModelConfigVO vo);

    List<GatewayRouteVO> listRoutes();

    void updateRoutes(List<GatewayRouteVO> routes);
}
