package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TenantUpdateDTO implements Serializable {
    @NotBlank(message = "学校名称不能为空")
    private String name;

    private String logo;

    private String domain;

    private String planCode;

    private LocalDateTime expireTime;

    private String adminName;

    private String adminPhone;

    private Integer status;
}
