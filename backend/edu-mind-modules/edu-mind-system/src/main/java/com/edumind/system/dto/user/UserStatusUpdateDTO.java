package com.edumind.system.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserStatusUpdateDTO implements Serializable {

    @NotBlank(message = "状态不能为空")
    private String status;
}
