package com.edumind.system.dto.menu;

import lombok.Data;

@Data
public class MenuUpdateDTO {
    private Long parentId;
    private String name;
    private Integer type;
    private String path;
    private String component;
    private String icon;
    private String permission;
    private Integer sort;
    private Integer status;
    private Boolean visible;
    private Boolean keepAlive;
}
