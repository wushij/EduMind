package com.edumind.system.vo.tenant;

import lombok.Data;

/**
 * 组织院系算力配额大盘 VO
 */
@Data
public class OrgQuotaVO {

    private Long orgId;
    private String name;
    private String orgType;
    private String orgTypeLabel;
    private String campusName;

    /**
     * AI Token 算力指标
     */
    private Long tokenLimit;
    private Long tokenUsed;
    private Integer usagePercent;
    private Integer warningThreshold;

    /**
     * 向量库存储指标 (MB)
     */
    private Long storageLimit;
    private Long storageUsed;

    /**
     * Agent 并发席位指标
     */
    private Long seatsLimit;
    private Long seatsUsed;

    /**
     * 配额健康状态 (NORMAL, WARNING, EXCEEDED)
     */
    private String status;
}
