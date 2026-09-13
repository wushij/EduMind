package com.edumind.system.dto.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通用配置分组更新 DTO
 */
@Data
public class SysConfigGroupDTO {

    /** 分组标识（如 site/session/rateLimit/login/register 等） */
    private String groupCode;

    /** 配置 JSON 文本内容 */
    @NotBlank(message = "配置值不能为空")
    private String configValue;
}
