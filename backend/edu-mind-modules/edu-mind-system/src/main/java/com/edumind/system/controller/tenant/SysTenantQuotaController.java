package com.edumind.system.controller.tenant;

import com.edumind.common.api.ApiResult;
import com.edumind.security.context.TenantContext;
import com.edumind.system.converter.TenantConverter;
import com.edumind.system.dao.SysTenantQuotaDao;
import com.edumind.system.entity.SysTenantQuotaEntity;
import com.edumind.system.vo.tenant.TenantQuotaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/tenant-quotas")
@RequiredArgsConstructor
public class SysTenantQuotaController {

    private final SysTenantQuotaDao sysTenantQuotaDao;
    private final TenantConverter tenantConverter;

    @GetMapping
    public ApiResult<List<TenantQuotaVO>> listQuotas(@RequestParam(required = false) Long tenantId) {
        if (tenantId == null) {
            tenantId = TenantContext.requireTenantId();
        }
        List<SysTenantQuotaEntity> list = sysTenantQuotaDao.listByTenantId(tenantId);
        return ApiResult.success(list.stream().map(tenantConverter::toQuotaVO).collect(Collectors.toList()));
    }

    @PutMapping
    public ApiResult<Void> updateQuotaThreshold(@RequestBody QuotaUpdateDTO dto) {
        Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : TenantContext.requireTenantId();
        SysTenantQuotaEntity entity = sysTenantQuotaDao.findByTenantAndType(tenantId, dto.getQuotaType());
        if (entity != null) {
            if (dto.getWarningThreshold() != null) {
                entity.setWarningThreshold(dto.getWarningThreshold());
            }
            if (dto.getLimitValue() != null) {
                entity.setLimitValue(dto.getLimitValue());
            }
            sysTenantQuotaDao.updateById(entity);
        }
        return ApiResult.success();
    }

    public static class QuotaUpdateDTO {
        private Long tenantId;
        private String quotaType;
        private Long limitValue;
        private Integer warningThreshold;

        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public String getQuotaType() { return quotaType; }
        public void setQuotaType(String quotaType) { this.quotaType = quotaType; }
        public Long getLimitValue() { return limitValue; }
        public void setLimitValue(Long limitValue) { this.limitValue = limitValue; }
        public Integer getWarningThreshold() { return warningThreshold; }
        public void setWarningThreshold(Integer warningThreshold) { this.warningThreshold = warningThreshold; }
    }
}
