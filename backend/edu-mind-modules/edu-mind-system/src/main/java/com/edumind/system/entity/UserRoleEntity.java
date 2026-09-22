package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户-角色关联（租户维度）
 * <p>tenantId = 0 表示平台级角色，对所有租户生效（ADMIN / PLATFORM_ADMIN / ROLE_ADMIN）；
 * tenantId &gt; 0 表示该授权仅在对应租户内生效，切换租户后权限随之变化。</p>
 * <p>注意：本表刻意不纳入 MyBatis-Plus 租户拦截器白名单，平台级行必须对所有租户可见，
 * 租户过滤由 {@code UserRoleDao} / {@code RoleDao} 显式完成。</p>
 */
@Data
@TableName("sys_user_role")
public class UserRoleEntity implements Serializable {

    /** 平台级授权标识：对所有租户生效 */
    public static final long PLATFORM_TENANT_ID = 0L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
    private Long tenantId;
}
