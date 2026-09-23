package com.edumind.ai.service.gateway;

import com.edumind.ai.vo.gateway.AiProviderPresetVO;
import com.edumind.ai.vo.gateway.AiProviderPresetsResponseVO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对齐 goblog-server aisvc/presets.go · DefaultProviderPresets。
 *
 * <p>2026-09-23 本地修正：MiniMax 的 API model 参数为小写（minimax-m3 / minimax-m2.7），
 * 原大写写法在调用端会 model not found；Kimi 补上最新的 kimi-k2.8-preview 与 kimi-k2.6。</p>
 */
@Component
public class AiProviderPresetCatalog {

    public AiProviderPresetsResponseVO getPresets() {
        AiProviderPresetsResponseVO response = new AiProviderPresetsResponseVO();
        response.setCatalogVersion("2026-09-23-v5");
        response.setChat(buildChatPresets());
        response.setEmbedding(buildEmbeddingPresets());
        return response;
    }

    private Map<String, AiProviderPresetVO> buildChatPresets() {
        Map<String, AiProviderPresetVO> map = new LinkedHashMap<>();
        map.put("deepseek", preset(
                "DeepSeek",
                "deepseek-v4-pro",
                "https://api.deepseek.com/v1",
                "https://platform.deepseek.com",
                "openai_compatible",
                List.of(
                        "deepseek-v4.1-flash",
                        "deepseek-flash",
                        "deepseek-v4-flash-vision-exp",
                        "deepseek-v4-pro",
                        "deepseek-v4-flash"
                )
        ));
        map.put("openai", preset(
                "OpenAI",
                "gpt-5",
                "https://api.openai.com/v1",
                "https://platform.openai.com",
                "openai_compatible",
                List.of("gpt-5.5", "gpt-5", "gpt-5-codex", "gpt-5.4-mini")
        ));
        map.put("qwen", preset(
                "Qwen（通义千问）",
                "qwen3.8-max",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "https://bailian.console.aliyun.com",
                "openai_compatible",
                List.of("qwen3.8-max", "qwen3.8-max-0902", "qwen3.8-flash")
        ));
        map.put("zhipu", preset(
                "GLM（智谱）",
                "glm-5.3",
                "https://open.bigmodel.cn/api/paas/v4",
                "https://open.bigmodel.cn",
                "openai_compatible",
                List.of("glm-5.3", "glm-5.3-flash")
        ));
        map.put("minimax", preset(
                "MiniMax",
                "minimax-m3",
                "https://api.minimaxi.com/v1",
                "https://platform.minimaxi.com",
                "openai_compatible",
                // 官方文档的展示名是大写 MiniMax-M3，但 API model 参数一律小写，
                // 填大写会导致调用时 model not found
                List.of("minimax-m3", "minimax-m2.7", "minimax-m2.7-highspeed")
        ));
        map.put("claude", preset(
                "Claude（Anthropic）",
                "claude-opus-5",
                "https://api.anthropic.com/v1",
                "https://console.anthropic.com",
                "anthropic_or_proxy",
                List.of(
                        "claude-opus-5",
                        "claude-sonnet-5",
                        "claude-fable-5-1",
                        "claude-haiku-4-5"
                )
        ));
        map.put("kimi", preset(
                "Kimi（月之暗面）",
                "kimi-k2.8-preview",
                "https://api.moonshot.cn/v1",
                "https://platform.moonshot.cn",
                "openai_compatible",
                List.of(
                        "kimi-k2.8-preview",
                        "kimi-k3",
                        "kimi-k2.7-code",
                        "kimi-k2.7-code-highspeed",
                        "kimi-k2.6"
                )
        ));
        map.put("mock", preset(
                "Mock（开发测试）",
                "mock-chat",
                "",
                "",
                "mock",
                List.of("mock-chat")
        ));
        return map;
    }

    private Map<String, AiProviderPresetVO> buildEmbeddingPresets() {
        Map<String, AiProviderPresetVO> map = new LinkedHashMap<>();
        map.put("openai", preset(
                "OpenAI Embedding",
                "text-embedding-3-large",
                "https://api.openai.com/v1",
                "https://platform.openai.com",
                "openai_compatible",
                List.of("text-embedding-3-large", "text-embedding-3-small")
        ));
        map.put("qwen", preset(
                "Qwen Embedding",
                "text-embedding-v4",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "https://bailian.console.aliyun.com",
                "openai_compatible",
                List.of("text-embedding-v4", "text-embedding-v3")
        ));
        return map;
    }

    private AiProviderPresetVO preset(String label, String modelName, String baseUrl, String portalUrl,
                                      String protocol, List<String> modelOptions) {
        AiProviderPresetVO vo = new AiProviderPresetVO();
        vo.setLabel(label);
        vo.setModelName(modelName);
        vo.setBaseUrl(baseUrl);
        vo.setPortalUrl(portalUrl);
        vo.setProtocol(protocol);
        vo.setModelOptions(modelOptions);
        return vo;
    }
}
