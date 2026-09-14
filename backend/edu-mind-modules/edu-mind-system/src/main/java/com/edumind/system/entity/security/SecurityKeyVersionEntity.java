package com.edumind.system.entity.security;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 国密密钥版本元数据实体
 */
@Data
@TableName("security_key_version")
public class SecurityKeyVersionEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String keyAlias;

    private Integer keyVersion;

    private String algorithm;

    /**
     * 状态 (ACTIVE / ROTATING / DEPRECATED)
     */
    private String status;

    private LocalDateTime activatedTime;
}
