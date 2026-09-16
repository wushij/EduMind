package com.edumind.system.api;

/**
 * 系统配置跨模块只读查询公开 API
 */
public interface SysConfigQueryApi {

    /**
     * 根据配置键获取配置值（例如 sys.ai.config）
     *
     * @param key 配置键
     * @return 配置值，若不存在返回 null
     */
    String getConfigValue(String key);

    /**
     * 根据配置分组编码获取配置 JSON 字符串（例如 ai, site, storage）
     *
     * @param groupCode 分组编码
     * @return 配置 JSON 字符串，若不存在返回 null
     */
    String getConfigValueByGroup(String groupCode);
}
