package com.edumind.system.vo.menu;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SysMenuVO {
    private Long id;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<SysMenuVO> children = new ArrayList<>();
}
