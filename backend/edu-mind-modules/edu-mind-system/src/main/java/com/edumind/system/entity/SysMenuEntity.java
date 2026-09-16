package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenuEntity implements Serializable {

    @TableId(type = IdType.AUTO)
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
    private Integer visible;
    private Integer keepAlive;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
