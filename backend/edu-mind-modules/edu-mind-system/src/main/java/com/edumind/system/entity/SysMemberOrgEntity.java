package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 成员组织分配关系映射实体
 */
@Data
@TableName("sys_member_org")
public class SysMemberOrgEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long organizationId;

    private String roleType; // HEAD_TEACHER / TEACHER / STUDENT
}
