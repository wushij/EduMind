package com.edumind.system.vo.tenant;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TenantDetailVO {
    private Long id;
    private String code;
    private String name;
    private String logo;
    private String domain;
    private String planCode;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private List<CampusVO> campuses;
    private List<TenantQuotaVO> quotas;
}
