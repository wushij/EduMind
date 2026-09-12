package com.edumind.ai.service.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.AiGatewayRouteDao;
import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.AiGatewayRouteEntity;
import com.edumind.ai.entity.AiModelConfigEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
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
    private final AiModelConfigDao aiModelConfigDao;
    private final AiGatewayRouteDao aiGatewayRouteDao;
    private final AiCallLogDao aiCallLogDao;

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
        return aiModelConfigDao.listAll().stream().map(this::toModelVo).collect(Collectors.toList());
    }

    @Override
    public List<AiModelConfigVO> listEnabledChatModels() {
        return aiModelConfigDao.listAll().stream()
                .filter(model -> Boolean.TRUE.equals(model.getEnabled()))
                .map(this::toModelVo)
                .collect(Collectors.toList());
    }

    @Override
    public void updateModel(Long id, AiModelConfigVO vo) {
        AiModelConfigEntity entity = aiModelConfigDao.findByModelKey(vo.getModelKey());
        if (entity == null) {
            entity = new AiModelConfigEntity();
            entity.setModelKey(vo.getModelKey());
        }
        entity.setProvider(vo.getProvider());
        entity.setEnabled(vo.getEnabled());
        entity.setPriority(vo.getPriority());
        entity.setFallbackModelKey(vo.getFallbackModelKey());
        entity.setMaxTokens(vo.getMaxTokens());
        entity.setTemperature(vo.getTemperature());
        if (entity.getId() != null) {
            aiModelConfigDao.updateById(entity);
        }
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

    private AiModelConfigVO toModelVo(AiModelConfigEntity entity) {
        AiModelConfigVO vo = new AiModelConfigVO();
        vo.setId(entity.getId());
        vo.setModelKey(entity.getModelKey());
        vo.setProvider(entity.getProvider());
        vo.setEnabled(entity.getEnabled());
        vo.setPriority(entity.getPriority());
        vo.setFallbackModelKey(entity.getFallbackModelKey());
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setTemperature(entity.getTemperature());
        return vo;
    }
}
