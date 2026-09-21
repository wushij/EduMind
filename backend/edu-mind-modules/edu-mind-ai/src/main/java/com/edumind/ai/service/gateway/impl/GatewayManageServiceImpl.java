package com.edumind.ai.service.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dao.AiGatewayRouteDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.entity.AiGatewayRouteEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.gateway.AiModelManageService;
import com.edumind.ai.service.gateway.GatewayManageService;
import com.edumind.ai.vo.audit.AiCallLogVO;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import com.edumind.ai.vo.gateway.GatewayMetricsVO;
import com.edumind.ai.vo.gateway.GatewayRouteVO;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.gateway.CircuitBreakerState;
import com.edumind.infrastructure.redis.gateway.GatewayResilienceStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayManageServiceImpl implements GatewayManageService {

    private final AiGatewayFacade aiGatewayFacade;
    private final AiGatewayRouteDao aiGatewayRouteDao;
    private final AiCallLogDao aiCallLogDao;
    private final AiModelManageService aiModelManageService;
    private final GatewayResilienceStore resilienceStore;
    private final AiConverter aiConverter;

    @Override
    public GatewayMetricsVO getMetrics(String range) {
        LocalDateTime since = resolveSince(range);
        AiGatewayFacade.GatewayMetrics snap = aiGatewayFacade.snapshot();

        // 1. 查询时间窗口内持久化的调用流水
        LambdaQueryWrapper<AiCallLogEntity> wrapper = new LambdaQueryWrapper<AiCallLogEntity>()
                .ge(since != null, AiCallLogEntity::getCreateTime, since)
                .orderByAsc(AiCallLogEntity::getCreateTime);
        List<AiCallLogEntity> logs = aiCallLogDao.list(wrapper);

        // 若当前窗口内无日志，但数据库存在历史全量日志，且用户选择非自定义范围，则回退为全量统计以防大盘完全空白
        boolean isWindowEmpty = logs.isEmpty();
        if (isWindowEmpty && ("all".equalsIgnoreCase(range) || "30d".equalsIgnoreCase(range))) {
            logs = aiCallLogDao.list(new LambdaQueryWrapper<AiCallLogEntity>().orderByAsc(AiCallLogEntity::getCreateTime));
        }

        GatewayMetricsVO vo = new GatewayMetricsVO();

        // 2. 核心量化指标计算
        long logCount = logs.size();
        long totalRequests = Math.max(logCount, snap.getTotalRequests());
        vo.setTotalRequests(totalRequests);

        long promptTokens = 0L;
        long completionTokens = 0L;
        List<Integer> latencies = new ArrayList<>();

        Map<String, ProviderAgg> providerMap = new HashMap<>();
        Map<String, SceneAgg> sceneMap = new HashMap<>();

        for (AiCallLogEntity logItem : logs) {
            int pTokens = logItem.getPromptTokens() != null ? Math.max(0, logItem.getPromptTokens()) : 0;
            int cTokens = logItem.getCompletionTokens() != null ? Math.max(0, logItem.getCompletionTokens()) : 0;
            promptTokens += pTokens;
            completionTokens += cTokens;

            if (logItem.getLatencyMs() != null && logItem.getLatencyMs() > 0) {
                latencies.add(logItem.getLatencyMs());
            }

            // 供应商 / 模型分组：优先按「命中的配置键」分组。
            // 多条配置可能指向同一上游型号（如 v4.1flash 与 Flash 都是 deepseek-v4-flash），
            // 只按型号分组会把不同配置合并成一行，无法判断实际走了哪条配置。
            String upstreamModel = StringUtils.hasText(logItem.getModel()) ? logItem.getModel() : "unknown";
            String groupKey = StringUtils.hasText(logItem.getModelKey()) ? logItem.getModelKey() : upstreamModel;
            ProviderAgg pAgg = providerMap.computeIfAbsent(groupKey, k -> new ProviderAgg());
            if (pAgg.modelKey == null) {
                pAgg.modelKey = StringUtils.hasText(logItem.getModelKey()) ? logItem.getModelKey() : null;
                pAgg.modelName = upstreamModel;
            }
            pAgg.calls++;
            pAgg.promptTokens += pTokens;
            pAgg.completionTokens += cTokens;
            pAgg.tokens += (pTokens + cTokens);
            if (logItem.getLatencyMs() != null && logItem.getLatencyMs() > 0) {
                pAgg.latencies.add(logItem.getLatencyMs());
            }

            // 业务场景分组
            String scene = StringUtils.hasText(logItem.getScene()) ? logItem.getScene() : "chat";
            SceneAgg sAgg = sceneMap.computeIfAbsent(scene, k -> new SceneAgg());
            sAgg.calls++;
            sAgg.tokens += (pTokens + cTokens);
            if (logItem.getLatencyMs() != null && logItem.getLatencyMs() > 0) {
                sAgg.latencies.add(logItem.getLatencyMs());
            }
        }

        long totalTokens = promptTokens + completionTokens;
        vo.setTotalTokens(totalTokens);
        vo.setPromptTokens(promptTokens);
        vo.setCompletionTokens(completionTokens);
        // 标准算力折合预估成本：千Token按 0.002 元基准核算
        vo.setEstimatedCost(Math.round(((totalTokens / 1000.0) * 0.002) * 100.0) / 100.0);

        // 3. 延迟与百分位
        if (!latencies.isEmpty()) {
            Collections.sort(latencies);
            double avg = latencies.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            vo.setAvgLatencyMs(Math.round(avg));
            int p95Idx = Math.min(latencies.size() - 1, (int) Math.ceil(latencies.size() * 0.95) - 1);
            int p99Idx = Math.min(latencies.size() - 1, (int) Math.ceil(latencies.size() * 0.99) - 1);
            vo.setP95LatencyMs((long) latencies.get(Math.max(0, p95Idx)));
            vo.setP99LatencyMs((long) latencies.get(Math.max(0, p99Idx)));
        } else {
            vo.setAvgLatencyMs(snap.getAvgLatencyMs());
            vo.setP95LatencyMs(snap.getAvgLatencyMs());
            vo.setP99LatencyMs(snap.getAvgLatencyMs());
        }

        // 4. 服务韧性指标（结合 Redis 韧性计数与快照）
        long fallbackCount = Math.max(resilienceStore.getMetric("fallbackCount"), snap.getFallbackCount());
        long circuitOpenCount = Math.max(resilienceStore.getMetric("circuitOpenCount"), snap.getCircuitOpenCount());
        long rateLimitedCount = Math.max(resilienceStore.getMetric("rateLimitedCount"), snap.getRateLimitedCount());
        long retryCount = Math.max(resilienceStore.getMetric("retryCount"), snap.getRetryCount());
        long failedCount = resilienceStore.getMetric("failedCount");

        vo.setFallbackCount(fallbackCount);
        vo.setCircuitOpenCount(circuitOpenCount);
        vo.setRateLimitedCount(rateLimitedCount);
        vo.setRetryCount(retryCount);

        // 成功率百分比：统一规范为 0.0 ~ 100.0 刻度
        if (totalRequests <= 0) {
            vo.setSuccessRate(100.0);
        } else {
            double rate = ((double) (totalRequests - Math.min(failedCount, totalRequests)) / totalRequests) * 100.0;
            vo.setSuccessRate(Math.max(0.0, Math.min(100.0, Math.round(rate * 10.0) / 10.0)));
        }

        // 5. 供应商 / 模型分布统计
        for (Map.Entry<String, ProviderAgg> entry : providerMap.entrySet()) {
            ProviderAgg agg = entry.getValue();
            GatewayMetricsVO.ProviderMetricVO pVO = new GatewayMetricsVO.ProviderMetricVO();
            // provider 保持"上游型号"语义（前端据此显示服务商品牌），modelKey 用于区分具体配置
            pVO.setProvider(agg.modelName != null ? agg.modelName : entry.getKey());
            pVO.setModelKey(agg.modelKey);
            pVO.setCalls(agg.calls);
            pVO.setTokens(agg.tokens);
            pVO.setPromptTokens(agg.promptTokens);
            pVO.setCompletionTokens(agg.completionTokens);
            pVO.setCost(Math.round(((agg.tokens / 1000.0) * 0.002) * 100.0) / 100.0);
            if (!agg.latencies.isEmpty()) {
                double avgLat = agg.latencies.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                pVO.setAvgLatencyMs(Math.round(avgLat));
            } else {
                pVO.setAvgLatencyMs(vo.getAvgLatencyMs());
            }
            pVO.setSuccessRate(100.0);
            vo.getByProvider().add(pVO);
        }
        vo.getByProvider().sort((a, b) -> Long.compare(b.getCalls(), a.getCalls()));

        // 6. 业务场景消耗分布统计
        for (Map.Entry<String, SceneAgg> entry : sceneMap.entrySet()) {
            SceneAgg agg = entry.getValue();
            GatewayMetricsVO.SceneMetricVO sVO = new GatewayMetricsVO.SceneMetricVO();
            sVO.setScene(entry.getKey());
            sVO.setSceneName(translateScene(entry.getKey()));
            sVO.setCalls(agg.calls);
            sVO.setTokens(agg.tokens);
            if (!agg.latencies.isEmpty()) {
                double avgLat = agg.latencies.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                sVO.setAvgLatencyMs(Math.round(avgLat));
            } else {
                sVO.setAvgLatencyMs(0L);
            }
            vo.getByScene().add(sVO);
        }
        vo.getByScene().sort((a, b) -> Long.compare(b.getCalls(), a.getCalls()));

        // 7. 动态构建连续时序趋势点 (ECharts 双轴趋势图)
        vo.setTimeSeriesTrend(buildTimeSeriesTrend(logs, range));

        // 8. 各活跃模型路由的熔断器状态机
        List<AiGatewayRouteEntity> routes = aiGatewayRouteDao.listAll();
        Set<String> modelKeys = new LinkedHashSet<>();
        for (AiGatewayRouteEntity r : routes) {
            if (StringUtils.hasText(r.getPrimaryModelKey())) modelKeys.add(r.getPrimaryModelKey());
            if (StringUtils.hasText(r.getFallbackModelKey())) modelKeys.add(r.getFallbackModelKey());
        }
        for (String key : modelKeys) {
            CircuitBreakerState cbState = resilienceStore.getCircuitState(key);
            GatewayMetricsVO.CircuitStateVO stateVO = new GatewayMetricsVO.CircuitStateVO();
            stateVO.setModelKey(key);
            stateVO.setStatus(cbState != null && cbState.getStatus() != null ? cbState.getStatus().name() : "CLOSED");
            stateVO.setOpenUntilMs(cbState != null ? cbState.getOpenUntilMs() : 0L);
            stateVO.setConsecutiveFailures(cbState != null ? cbState.getConsecutiveFailures() : 0);
            vo.getCircuitStates().add(stateVO);
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
            throw new BusinessException("配置名称不能为空");
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

    @Override
    public PageResult<AiCallLogVO> listGatewayLogs(String scene, String model, long page, long pageSize) {
        LambdaQueryWrapper<AiCallLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(scene)) {
            wrapper.eq(AiCallLogEntity::getScene, scene.trim());
        }
        if (StringUtils.hasText(model)) {
            wrapper.like(AiCallLogEntity::getModel, model.trim());
        }
        wrapper.orderByDesc(AiCallLogEntity::getCreateTime);
        Page<AiCallLogEntity> pageResult = aiCallLogDao.page(new Page<>(page, pageSize), wrapper);
        List<AiCallLogVO> voList = pageResult.getRecords().stream()
                .map(aiConverter::toCallLogVO)
                .collect(Collectors.toList());
        return PageResult.<AiCallLogVO>builder()
                .total(pageResult.getTotal())
                .pageNum(page)
                .pageSize(pageSize)
                .list(voList)
                .build();
    }

    @Override
    public void resetCircuit(String modelKey) {
        if (StringUtils.hasText(modelKey)) {
            resilienceStore.resetCircuit(modelKey.trim());
            log.info("Reset gateway circuit breaker for modelKey={}", modelKey);
        } else {
            resilienceStore.resetCircuit("default");
            log.info("Reset gateway default circuit breaker");
        }
    }

    private LocalDateTime resolveSince(String range) {
        if ("1h".equalsIgnoreCase(range)) {
            return LocalDateTime.now().minusHours(1);
        }
        if ("7d".equalsIgnoreCase(range)) {
            return LocalDateTime.now().minusDays(7);
        }
        if ("30d".equalsIgnoreCase(range)) {
            return LocalDateTime.now().minusDays(30);
        }
        if ("all".equalsIgnoreCase(range)) {
            return null;
        }
        // 默认近 24 小时
        return LocalDateTime.now().minusHours(24);
    }

    private List<GatewayMetricsVO.TimeSeriesPointVO> buildTimeSeriesTrend(List<AiCallLogEntity> logs, String range) {
        List<GatewayMetricsVO.TimeSeriesPointVO> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if ("1h".equalsIgnoreCase(range)) {
            // 12 个 5 分钟槽位
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            for (int i = 11; i >= 0; i--) {
                LocalDateTime startSlot = now.minusMinutes((i + 1) * 5L);
                LocalDateTime endSlot = now.minusMinutes(i * 5L);
                long calls = 0;
                long tokens = 0;
                long latencySum = 0;
                long latencyCount = 0;
                for (AiCallLogEntity item : logs) {
                    if (item.getCreateTime() != null && !item.getCreateTime().isBefore(startSlot) && item.getCreateTime().isBefore(endSlot)) {
                        calls++;
                        tokens += (item.getPromptTokens() != null ? item.getPromptTokens() : 0)
                                + (item.getCompletionTokens() != null ? item.getCompletionTokens() : 0);
                        if (item.getLatencyMs() != null && item.getLatencyMs() > 0) {
                            latencySum += item.getLatencyMs();
                            latencyCount++;
                        }
                    }
                }
                GatewayMetricsVO.TimeSeriesPointVO p = new GatewayMetricsVO.TimeSeriesPointVO();
                p.setTime(endSlot.format(fmt));
                p.setCalls(calls);
                p.setTokens(tokens);
                p.setAvgLatencyMs(latencyCount > 0 ? latencySum / latencyCount : 0L);
                trend.add(p);
            }
        } else if ("7d".equalsIgnoreCase(range) || "30d".equalsIgnoreCase(range)) {
            int days = "30d".equalsIgnoreCase(range) ? 30 : 7;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = days - 1; i >= 0; i--) {
                LocalDateTime dayStart = now.minusDays(i).toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1);
                long calls = 0;
                long tokens = 0;
                long latencySum = 0;
                long latencyCount = 0;
                for (AiCallLogEntity item : logs) {
                    if (item.getCreateTime() != null && !item.getCreateTime().isBefore(dayStart) && item.getCreateTime().isBefore(dayEnd)) {
                        calls++;
                        tokens += (item.getPromptTokens() != null ? item.getPromptTokens() : 0)
                                + (item.getCompletionTokens() != null ? item.getCompletionTokens() : 0);
                        if (item.getLatencyMs() != null && item.getLatencyMs() > 0) {
                            latencySum += item.getLatencyMs();
                            latencyCount++;
                        }
                    }
                }
                GatewayMetricsVO.TimeSeriesPointVO p = new GatewayMetricsVO.TimeSeriesPointVO();
                p.setTime(dayStart.format(fmt));
                p.setCalls(calls);
                p.setTokens(tokens);
                p.setAvgLatencyMs(latencyCount > 0 ? latencySum / latencyCount : 0L);
                trend.add(p);
            }
        } else {
            // 24 小时：24 个整点槽位
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:00");
            for (int i = 23; i >= 0; i--) {
                LocalDateTime hourStart = now.minusHours(i).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime hourEnd = hourStart.plusHours(1);
                long calls = 0;
                long tokens = 0;
                long latencySum = 0;
                long latencyCount = 0;
                for (AiCallLogEntity item : logs) {
                    if (item.getCreateTime() != null && !item.getCreateTime().isBefore(hourStart) && item.getCreateTime().isBefore(hourEnd)) {
                        calls++;
                        tokens += (item.getPromptTokens() != null ? item.getPromptTokens() : 0)
                                + (item.getCompletionTokens() != null ? item.getCompletionTokens() : 0);
                        if (item.getLatencyMs() != null && item.getLatencyMs() > 0) {
                            latencySum += item.getLatencyMs();
                            latencyCount++;
                        }
                    }
                }
                GatewayMetricsVO.TimeSeriesPointVO p = new GatewayMetricsVO.TimeSeriesPointVO();
                p.setTime(hourStart.format(fmt));
                p.setCalls(calls);
                p.setTokens(tokens);
                p.setAvgLatencyMs(latencyCount > 0 ? latencySum / latencyCount : 0L);
                trend.add(p);
            }
        }
        return trend;
    }

    private String translateScene(String scene) {
        if (scene == null) return "未知场景";
        return switch (scene.toLowerCase()) {
            case "chat" -> "课程智能助教答疑";
            case "global_assistant" -> "全局教学助手";
            case "learning" -> "AI 自适应学习辅导";
            case "memory_extract" -> "学情长期记忆沉淀";
            case "chat_rag", "rag", "knowledge", "kb_retrieval" -> "RAG 知识检索增强";
            case "question", "question_generate" -> "AI 题库出题与变式";
            case "grading", "subjective_grading" -> "作业/主观题智能批改";
            case "prep", "lesson_plan" -> "AI 智能备课教案";
            case "exam", "paper_compose" -> "智能组卷与试题分析";
            case "evaluation" -> "学情综合诊断评估";
            case "teaching_advice" -> "AI 学情诊断与教学建议";
            case "course_objective" -> "课程教学目标 AI 推荐";
            case "course_description" -> "课程简介 AI 生成";
            case "course_knowledge_point" -> "课程知识点 AI 推荐";
            case "agent" -> "Agent 多步任务规划";
            case "stream" -> "流式启发式对话";
            case "ocr" -> "智能 OCR 文本识别";
            case "embedding" -> "知识向量化嵌入";
            case "rerank" -> "语义重排检索优化";
            default -> scene;
        };
    }

    private static class ProviderAgg {
        /** 命中的配置键；历史日志为空时回退为上游型号 */
        String modelKey;
        /** 上游型号（展示用） */
        String modelName;
        long calls;
        long tokens;
        long promptTokens;
        long completionTokens;
        List<Integer> latencies = new ArrayList<>();
    }

    private static class SceneAgg {
        long calls;
        long tokens;
        List<Integer> latencies = new ArrayList<>();
    }
}
