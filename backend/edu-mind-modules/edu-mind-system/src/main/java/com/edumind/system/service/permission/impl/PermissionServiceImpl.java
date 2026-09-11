package com.edumind.system.service.permission.impl;

import com.edumind.system.converter.PermissionConverter;
import com.edumind.system.dao.PermissionDao;
import com.edumind.system.service.permission.PermissionService;
import com.edumind.system.vo.permission.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionDao permissionDao;
    private final PermissionConverter permissionConverter;

    @Override
    public List<PermissionVO> getPermissionTree() {
        return permissionConverter.toTree(permissionDao.findAll());
    }
}
