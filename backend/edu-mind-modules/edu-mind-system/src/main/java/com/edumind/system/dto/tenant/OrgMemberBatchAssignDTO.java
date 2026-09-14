package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量分配组织成员 DTO
 */
@Data
public class OrgMemberBatchAssignDTO implements Serializable {

    @NotEmpty(message = "待分配成员列表不能为空")
    private List<Long> memberIds;

    /**
     * 组织内角色 (STUDENT: 学生 / TEACHER: 任课教师 / HEAD_TEACHER: 班主任 / MONITOR: 班长)
     */
    private String roleType;
}
