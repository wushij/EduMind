package com.edumind.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.system.entity.SysTenantQuotaEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysTenantQuotaMapper extends BaseMapper<SysTenantQuotaEntity> {

    @Update("UPDATE sys_tenant_quota SET used_value = used_value + #{delta}, update_time = NOW() " +
            "WHERE tenant_id = #{tenantId} AND quota_type = #{quotaType} AND used_value + #{delta} <= limit_value")
    int consumeQuotaAtomic(@Param("tenantId") Long tenantId, @Param("quotaType") String quotaType, @Param("delta") long delta);
}
