package com.edumind.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 密码重置提交入参
 * 支持两种模式：
 * 1. 票据模式：resetToken + newPassword（向导步骤推荐）
 * 2. 直填模式：email + code + newPassword（兼容参考项目 Cloud_Disk 结构）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetDTO implements Serializable {

    /**
     * 重置密码临时安全票据
     */
    private String resetToken;

    /**
     * 绑定的电子邮箱（直填模式）
     */
    private String email;

    /**
     * 邮箱验证码（直填模式）
     */
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码不能少于6位")
    private String newPassword;
}
