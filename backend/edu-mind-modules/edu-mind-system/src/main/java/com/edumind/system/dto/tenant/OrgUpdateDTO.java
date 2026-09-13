package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 组织机构节点更新 DTO
 */
@Data
public class OrgUpdateDTO {

    /**
     * 组织机构名称
     */
    @NotBlank(message = "组织名称不能为空")
    private String name;

    /**
     * 排序值
     */
    private Integer sortOrder;
}
