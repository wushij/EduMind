package com.edumind.system.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 组织机构节点创建 DTO
 */
@Data
public class OrgCreateDTO {

    /**
     * 组织机构名称
     */
    @NotBlank(message = "组织名称不能为空")
    private String name;

    /**
     * 组织节点类型 (CAMPUS/COLLEGE/MAJOR/CLASS)
     */
    @NotBlank(message = "组织类型不能为空")
    private String orgType;

    /**
     * 上级组织 ID (根节点传 0)
     */
    private Long parentId;

    /**
     * 排序值
     */
    private Integer sortOrder;
}
