package com.edumind.ai.service.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.AiGatewayRouteDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.AiGatewayRouteEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.gateway.AiModelManageService;
import com.edumind.ai.service.gateway.GatewayManageService;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import com.edumind.ai.vo.gateway.GatewayMetricsVO;
import com.edumind.ai.vo.gateway.GatewayRouteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GatewayManageServiceImpl implements GatewayManageService {

    private final AiGatewayFacade aiGatewayFacade;
    private final AiGatewayRouteDao aiGatewayRouteDao;
    private final AiCallLogDao aiCallLogDao;
    private final AiModelManageService aiModelManageService;

    @Override
    public GatewayMetricsVO getMetrics(String range) {
        AiGatewayFacade.GatewayMetrics snap = aiGatewayFacade.snapshot();
        GatewayMetricsVO vo = new GatewayMetricsVO();
        vo.setTotalRequests(snap.getTotalRequests());
        vo.setSuccessRate(snap.getSuccessRate());
        vo.setAvgLatencyMs(snap.getAvgLatencyMs());
        vo.setFallbackCount(snap.getFallbackCount());
        vo.setCircuitOpenCount(snap.getCircuitOpenCount());
        vo.setRateLimitedCount(snap.getRateLimitedCount());
        vo.setRetryCount(snap.getRetryCount());
        List<AiCallLogEntity> logs = aiCallLogDao.list(new LambdaQueryWrapper<>());
        Map<String, Long> calls = new HashMap<>();
        Map<String, Long> tokens = new HashMap<>();
        for (AiCallLogEntity log : logs) {
            String provider = log.getModel() != null ? log.getModel() : "unknown";
            calls.merge(provider, 1L, Long::sum);
            long t = (log.getPromptTokens() != null ? log.getPromptTokens() : 0)
                    + (log.getCompletionTokens() != null ? log.getCompletionTokens() : 0);
            tokens.merge(provider, t, Long::sum);
        }
        for (Map.Entry<String, Long> e : calls.entrySet()) {
            GatewayMetricsVO.ProviderMetricVO p = new GatewayMetricsVO.ProviderMetricVO();
            p.setProvider(e.getKey());
            p.setCalls(e.getValue());
            p.setTokens(tokens.getOrDefault(e.getKey(), 0L));
            vo.getByProvider().add(p);
        }
        return vo;
    }

    @Override
    public List<AiModelConfigVO> listModels() {
        return aiModelManageService.listModels(null, null);
    }

    @Override
    public List<AiModelConfigVO> listEnabledChatModels() {
        return aiModelManageService.listEnabledChatModels();
    }

    @Override
    public void updateModel(Long id, AiModelConfigVO vo) {
        if (vo.getName() == null) {
            throw new com.edumind.common.exception.BusinessException("配置名称不能为空");
        }
        com.edumind.ai.dto.gateway.AiModelSaveDTO dto = new com.edumind.ai.dto.gateway.AiModelSaveDTO();
        dto.setName(vo.getName());
        dto.setProvider(vo.getProvider());
        dto.setConfigType(vo.getConfigType());
        dto.setModelName(vo.getModelName());
        dto.setBaseUrl(vo.getBaseUrl());
        dto.setApiKey(vo.getApiKey());
        dto.setTemperature(vo.getTemperature());
        dto.setReasoningEffort(vo.getReasoningEffort());
        dto.setDimension(vo.getDimension());
        dto.setStatus(vo.getStatus());
        dto.setIsDefault(vo.getIsDefault());
        dto.setSortOrder(vo.getSortOrder());
        aiModelManageService.updateModel(vo.getName(), dto);
    }

    @Override
    public List<GatewayRouteVO> listRoutes() {
        return aiGatewayRouteDao.listAll().stream().map(r -> {
            GatewayRouteVO vo = new GatewayRouteVO();
            vo.setScene(r.getScene());
            vo.setPrimaryModelKey(r.getPrimaryModelKey());
            vo.setFallbackModelKey(r.getFallbackModelKey());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateRoutes(List<GatewayRouteVO> routes) {
        for (GatewayRouteVO route : routes) {
            AiGatewayRouteEntity entity = aiGatewayRouteDao.findByScene(route.getScene());
            if (entity != null) {
                entity.setPrimaryModelKey(route.getPrimaryModelKey());
                entity.setFallbackModelKey(route.getFallbackModelKey());
                aiGatewayRouteDao.updateById(entity);
            }
        }
    }
}
