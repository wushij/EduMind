package com.edumind.ai.controller.quota;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dao.SysAiQuotaDao;
import com.edumind.ai.entity.SysAiQuotaEntity;
import com.edumind.common.api.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/ai-quota")
@RequiredArgsConstructor
public class AiQuotaController {

    private final SysAiQuotaDao sysAiQuotaDao;

    @SaCheckPermission("system:quota:view")
    @GetMapping
    public ApiResult<List<SysAiQuotaEntity>> list() {
        return ApiResult.success(sysAiQuotaDao.listAll());
    }

    @SaCheckPermission("system:quota:edit")
    @PutMapping("/{userId}")
    public ApiResult<Void> update(@PathVariable Long userId, @RequestBody SysAiQuotaEntity body) {
        SysAiQuotaEntity existing = sysAiQuotaDao.findByUserId(userId);
        if (existing == null) {
            body.setUserId(userId);
            sysAiQuotaDao.insert(body);
        } else {
            existing.setDailyTokenLimit(body.getDailyTokenLimit());
            existing.setDailyCallLimit(body.getDailyCallLimit());
            sysAiQuotaDao.updateById(existing);
        }
        return ApiResult.success();
    }
}
