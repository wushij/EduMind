package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantCreateDTO {
    @NotBlank(message = "学校编码不能为空")
    private String code;
    @NotBlank(message = "学校名称不能为空")
    private String name;
    private String logo;
    private String domain;
    private String planCode;
    private LocalDateTime expireTime;
}
