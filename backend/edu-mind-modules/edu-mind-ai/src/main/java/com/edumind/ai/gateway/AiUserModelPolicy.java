package com.edumind.ai.gateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.system.api.SysConfigQueryApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户自选模型治理策略。
 *
 * <p>平台级配置来自 {@code sys_config} 的「ai」分组（组内 JSON）：</p>
 * <ul>
 *   <li>{@code allowUserModelSwitch}：是否允许普通用户自选模型，缺省 true（兼容历史行为）；</li>
 *   <li>{@code userSelectableModelKeys}：允许用户自选的模型配置键白名单；为空表示"所有已启用的对话模型"。</li>
 * </ul>
 *
 * <p>作用：用户/会话的显式选择必须经过这里校验才允许生效，
 * 否则回落平台策略（场景路由 → 平台默认），避免用户把批改/对话切到平台未开放（例如成本过高）的模型。</p>
 */
@Slf4j
@Component
public class AiUserModelPolicy {

    private static final String AI_CONFIG_GROUP = "ai";
    private static final String KEY_USER_SWITCH = "allowUserModelSwitch";
    private static final String KEY_SELECTABLE = "userSelectableModelKeys";

    @Autowired(required = false)
    private SysConfigQueryApi sysConfigQueryApi;

    /** 平台是否允许用户自选模型；配置缺失时按允许处理，保持与历史行为一致 */
    public boolean isUserSwitchAllowed() {
        JSONObject config = loadAiConfig();
        if (config == null) {
            return true;
        }
        Boolean allowed = config.getBoolean(KEY_USER_SWITCH);
        return allowed == null || allowed;
    }

    /** 用户可选模型白名单；为空表示不限制（所有已启用对话模型可选） */
    public Set<String> userSelectableModelKeys() {
        Set<String> keys = new LinkedHashSet<>();
        JSONObject config = loadAiConfig();
        if (config == null) {
            return keys;
        }
        JSONArray array = config.getJSONArray(KEY_SELECTABLE);
        if (array == null) {
            return keys;
        }
        for (int i = 0; i < array.size(); i++) {
            String key = array.getString(i);
            if (StringUtils.hasText(key)) {
                keys.add(key.trim());
            }
        }
        return keys;
    }

    /**
     * 校验用户显式选择的模型是否允许生效。
     *
     * @return 允许时返回规范化后的配置键；不允许（平台关闭自选 / 不在白名单）时返回 null，由调用方回落平台策略
     */
    public String validateUserSelection(String modelKey) {
        if (!StringUtils.hasText(modelKey)) {
            return null;
        }
        String candidate = modelKey.trim();
        if (!isUserSwitchAllowed()) {
            log.info("[模型治理] 平台已关闭用户自选模型，忽略用户选择 {}，回落平台场景策略", candidate);
            return null;
        }
        Set<String> whitelist = userSelectableModelKeys();
        if (whitelist.isEmpty()) {
            return candidate;
        }
        boolean allowed = whitelist.stream().anyMatch(key -> key.equalsIgnoreCase(candidate));
        if (!allowed) {
            log.warn("[模型治理] 用户选择的模型 {} 不在平台白名单内，回落平台场景策略", candidate);
            return null;
        }
        return candidate;
    }

    /** 列表接口过滤用：判断某模型是否允许用户选择 */
    public boolean isSelectable(String modelKey) {
        if (!StringUtils.hasText(modelKey)) {
            return false;
        }
        if (!isUserSwitchAllowed()) {
            return false;
        }
        Set<String> whitelist = userSelectableModelKeys();
        if (whitelist.isEmpty()) {
            return true;
        }
        String candidate = modelKey.trim();
        return whitelist.stream().anyMatch(key -> key.equalsIgnoreCase(candidate));
    }

    private JSONObject loadAiConfig() {
        if (sysConfigQueryApi == null) {
            return null;
        }
        try {
            String json = sysConfigQueryApi.getConfigValueByGroup(AI_CONFIG_GROUP);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return JSON.parseObject(json);
        } catch (Exception ex) {
            log.warn("读取 AI 平台配置失败，按缺省策略处理: {}", ex.getMessage());
            return null;
        }
    }
}
