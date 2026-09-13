package com.edumind.system.vo.tenant;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantListVO {
    private Long id;
    private String code;
    private String name;
    private String logo;
    private String domain;
    private String planCode;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private Integer campusCount;
    private Long tokenUsagePercent;
}
