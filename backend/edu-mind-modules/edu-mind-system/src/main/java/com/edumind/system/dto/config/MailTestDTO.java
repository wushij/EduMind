package com.edumind.system.dto.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件 SMTP 服务连通性测试入参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailTestDTO implements Serializable {

    @NotBlank(message = "测试收信邮箱不能为空")
    @Email(message = "请输入合法的电子邮箱格式")
    private String toEmail;
}
