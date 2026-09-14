package com.edumind.system.vo.tenant;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CampusVO {
    private Long id;
    private Long tenantId;
    private String code;
    private String campusCode;
    private String name;
    private String address;
    private Boolean isMain;
    private Integer status;
    private LocalDateTime createTime;
}
