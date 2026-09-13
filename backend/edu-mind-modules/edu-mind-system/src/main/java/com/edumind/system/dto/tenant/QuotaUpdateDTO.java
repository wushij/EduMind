package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 租户配额配置更新 DTO
 */
@Data
public class QuotaUpdateDTO {

    /**
     * 目标租户 ID (仅平台超管可跨租户指定，普通租户管理员忽略此字段直接取当前租户)
     */
    private Long tenantId;

    /**
     * 配额类型 (TOKEN / STORAGE / QPS / SEATS)
     */
    @NotBlank(message = "配额类型不能为空")
    private String quotaType;

    /**
     * 限制上限值
     */
    private Long limitValue;

    /**
     * 预警阈值百分比 (如 85)
     */
    private Integer warningThreshold;
}
