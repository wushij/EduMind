package com.edumind.system.vo.tenant;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrganizationNodeVO {
    private Long id;
    private Long tenantId;
    private Long parentId;
    private String orgType;
    private String orgPath;
    private String name;
    private Integer sortOrder;
    private Integer memberCount;
    private List<OrganizationNodeVO> children = new ArrayList<>();
}
