package com.edumind.system.dto.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 测试生成支付订单 DTO
 */
@Data
public class TestPaymentDTO {

    /** 支付类型：wechat 或 alipay */
    @NotBlank(message = "支付类型不能为空")
    private String type;
}
