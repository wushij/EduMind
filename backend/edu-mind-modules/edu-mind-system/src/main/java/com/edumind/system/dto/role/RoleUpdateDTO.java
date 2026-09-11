package com.edumind.system.dto.role;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RoleUpdateDTO implements Serializable {

    private String roleName;
    private String description;
    private List<Long> permissionIds;
}
