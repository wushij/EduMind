package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学校租户持久化实体
 */
@Data
@TableName("sys_tenant")
public class SysTenantEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String logo;
    private String domain;
    private String planCode;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
