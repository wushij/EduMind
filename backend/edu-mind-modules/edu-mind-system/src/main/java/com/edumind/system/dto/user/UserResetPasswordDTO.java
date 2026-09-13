package com.edumind.system.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员重置用户密码 DTO
 */
@Data
public class UserResetPasswordDTO implements Serializable {

    @NotBlank(message = "重置后的新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需在 6 到 32 个字符之间")
    private String newPassword;
}
