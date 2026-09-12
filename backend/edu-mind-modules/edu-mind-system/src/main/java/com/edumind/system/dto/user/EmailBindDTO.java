package com.edumind.system.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户绑定或更换邮箱入参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailBindDTO implements Serializable {

    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "请输入有效的电子邮箱格式")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
