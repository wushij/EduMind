package com.edumind.system.vo.tenant;

import lombok.Data;

@Data
public class CampusVO {
    private Long id;
    private Long tenantId;
    private String code;
    private String name;
    private String address;
    private Integer status;
}
