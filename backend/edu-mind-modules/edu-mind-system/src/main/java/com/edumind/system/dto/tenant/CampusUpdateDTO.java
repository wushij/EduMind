package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class CampusUpdateDTO implements Serializable {
    @NotBlank(message = "校区名称不能为空")
    private String name;

    private String address;

    private Boolean isMain;

    private Integer status;
}
