package com.edumind.system.service.permission;

import com.edumind.system.vo.permission.PermissionVO;

import java.util.List;

public interface PermissionService {

    List<PermissionVO> getPermissionTree();
}
