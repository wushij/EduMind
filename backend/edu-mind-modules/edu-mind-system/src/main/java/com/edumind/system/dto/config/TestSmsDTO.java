package com.edumind.system.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 测试发送短信 DTO
 */
@Data
public class TestSmsDTO {

    /** 目标手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位中国大陆手机号")
    private String phone;

    /** 短信模板ID/CODE（选填） */
    private String templateCode;
}
