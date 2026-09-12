package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统全局参数配置实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_config")
public class SysConfigEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键（唯一识别标示） */
    private String configKey;

    /** 配置值（支持 JSON 或长文本） */
    private String configValue;

    /** 配置中文友好名称 */
    private String configName;

    /** 分组标识（mail/security/base等） */
    private String configGroup;

    /** 备注说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
