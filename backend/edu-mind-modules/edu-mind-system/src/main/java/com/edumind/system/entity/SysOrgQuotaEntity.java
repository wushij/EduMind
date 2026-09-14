package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织院系算力配额实体
 */
@Data
@TableName("sys_org_quota")
public class SysOrgQuotaEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long orgId;
    private String quotaType;
    private Long limitValue;
    private Long usedValue;
    private Integer warningThreshold;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
