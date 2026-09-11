package com.edumind.system.vo.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO implements Serializable {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private Long parentId;

    @Builder.Default
    private List<PermissionVO> children = new ArrayList<>();
}
