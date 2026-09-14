package com.edumind.infrastructure.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 短信发信服务配置（与 sys.sms.config JSON 字段对应）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsConfig implements Serializable {

    @Builder.Default
    private Boolean enabled = false;

    /** aliyunAuth / tencent / console */
    @Builder.Default
    private String provider = "aliyunAuth";

    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String tencentAppId;

    @Builder.Default
    private String templateVerifyCode = "100001";

    private String templateModifyPhone;
    private String templateResetPassword;
    private String templateBindPhone;
    private String templateVerifyBindPhone;
    private String schemeName;

    @Builder.Default
    private Integer codeExpireMinutes = 5;
}
