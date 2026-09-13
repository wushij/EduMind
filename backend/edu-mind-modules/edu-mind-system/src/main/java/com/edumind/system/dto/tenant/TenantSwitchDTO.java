package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantSwitchDTO {
    @NotNull(message = "目标租户ID不能为空")
    private Long targetTenantId;
    /**
     * 若为代管会话，说明代管申请原因
     */
    private String reason;
}
