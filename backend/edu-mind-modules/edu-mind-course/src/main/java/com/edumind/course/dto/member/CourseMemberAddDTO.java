package com.edumind.course.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class CourseMemberAddDTO implements Serializable {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "成员角色不能为空")
    private String role;
}
