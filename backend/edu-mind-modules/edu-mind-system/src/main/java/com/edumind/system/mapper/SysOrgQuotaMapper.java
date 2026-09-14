package com.edumind.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.system.entity.SysOrgQuotaEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 组织院系算力配额 Mapper
 */
@Mapper
public interface SysOrgQuotaMapper extends BaseMapper<SysOrgQuotaEntity> {

    @Update("UPDATE sys_org_quota SET used_value = used_value + #{delta}, update_time = NOW() " +
            "WHERE tenant_id = #{tenantId} AND org_id = #{orgId} AND quota_type = #{quotaType}")
    int consumeOrgQuotaAtomic(@Param("tenantId") Long tenantId,
                              @Param("orgId") Long orgId,
                              @Param("quotaType") String quotaType,
                              @Param("delta") long delta);
}
