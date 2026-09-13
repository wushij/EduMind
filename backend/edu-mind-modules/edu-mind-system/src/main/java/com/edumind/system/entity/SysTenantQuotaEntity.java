package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_tenant_quota")
public class SysTenantQuotaEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String quotaType;
    private Long limitValue;
    private Long usedValue;
    private Integer warningThreshold;
    private LocalDateTime updateTime;
}
