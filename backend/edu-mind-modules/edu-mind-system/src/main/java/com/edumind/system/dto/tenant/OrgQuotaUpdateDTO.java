package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组织院系算力配额更新 DTO
 */
@Data
public class OrgQuotaUpdateDTO {

    /**
     * 目标租户 ID (可选，默认取当前上下文)
     */
    private Long tenantId;

    /**
     * 组织节点 ID
     */
    @NotNull(message = "组织节点ID不能为空")
    private Long orgId;

    /**
     * AI Token 算力分配额度上限
     */
    private Long tokenLimit;

    /**
     * 向量知识库存储上限 (MB)
     */
    private Long storageLimit;

    /**
     * Agent 最大并发席位数
     */
    private Long seatsLimit;

    /**
     * 预警阈值水位线百分比 (默认 85)
     */
    private Integer warningThreshold;
}
