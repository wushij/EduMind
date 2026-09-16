package com.edumind.system.api.impl;

import com.edumind.system.api.SysConfigQueryApi;
import com.edumind.system.service.config.SysConfigService;
import com.edumind.system.vo.config.SysConfigGroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统配置跨模块查询实现
 */
@Service
@RequiredArgsConstructor
public class SysConfigQueryApiImpl implements SysConfigQueryApi {

    private final SysConfigService sysConfigService;

    @Override
    public String getConfigValue(String key) {
        return sysConfigService.getConfigValue(key);
    }

    @Override
    public String getConfigValueByGroup(String groupCode) {
        SysConfigGroupVO vo = sysConfigService.getByGroupCode(groupCode);
        return vo != null ? vo.getConfigValue() : null;
    }
}
