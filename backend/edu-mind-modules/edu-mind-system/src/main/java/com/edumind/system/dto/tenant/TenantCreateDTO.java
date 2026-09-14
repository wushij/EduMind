package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantCreateDTO {
    private String code;
    @NotBlank(message = "学校名称不能为空")
    private String name;
    private String tenantCode;
    private String logo;
    private String domain;
    private String planCode;
    private LocalDateTime expireTime;
    private String adminName;
    private String adminPhone;
    private String adminPassword;

    public String getEffectiveCode() {
        if (code != null && !code.isBlank()) return code;
        return tenantCode;
    }
}
