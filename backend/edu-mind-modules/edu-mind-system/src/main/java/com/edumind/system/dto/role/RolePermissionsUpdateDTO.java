package com.edumind.system.dto.role;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RolePermissionsUpdateDTO implements Serializable {

    private List<Long> permissionIds;
}
