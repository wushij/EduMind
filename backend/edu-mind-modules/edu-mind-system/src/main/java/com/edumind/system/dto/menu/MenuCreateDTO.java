package com.edumind.system.dto.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MenuCreateDTO {
    @NotNull
    private Long parentId;
    @NotBlank
    private String name;
    @NotNull
    private Integer type;
    private String path;
    private String component;
    private String icon;
    private String permission;
    @NotNull
    private Integer sort;
    @NotNull
    private Integer status;
    private Boolean visible;
    private Boolean keepAlive;
}
