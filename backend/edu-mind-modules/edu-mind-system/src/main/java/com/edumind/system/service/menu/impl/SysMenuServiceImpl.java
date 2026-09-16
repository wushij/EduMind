package com.edumind.system.service.menu.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.system.dao.SysMenuDao;
import com.edumind.system.dto.menu.MenuCreateDTO;
import com.edumind.system.dto.menu.MenuUpdateDTO;
import com.edumind.system.entity.SysMenuEntity;
import com.edumind.system.service.menu.SysMenuService;
import com.edumind.system.vo.menu.SysMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuDao sysMenuDao;

    @Override
    public List<SysMenuVO> getTree(String keyword) {
        List<SysMenuEntity> flat = sysMenuDao.listActive(keyword);
        List<SysMenuVO> vos = flat.stream().map(this::toVo).collect(Collectors.toList());
        Map<Long, List<SysMenuVO>> byParent = vos.stream()
                .collect(Collectors.groupingBy(v -> v.getParentId() == null ? 0L : v.getParentId()));
        vos.forEach(v -> {
            List<SysMenuVO> children = byParent.getOrDefault(v.getId(), new ArrayList<>());
            children.sort(Comparator.comparing(SysMenuVO::getSort, Comparator.nullsLast(Integer::compareTo)));
            v.setChildren(children);
        });
        List<SysMenuVO> roots = byParent.getOrDefault(0L, new ArrayList<>());
        roots.sort(Comparator.comparing(SysMenuVO::getSort, Comparator.nullsLast(Integer::compareTo)));
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuVO create(MenuCreateDTO dto) {
        SysMenuEntity entity = new SysMenuEntity();
        entity.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setPath(dto.getPath());
        entity.setComponent(dto.getComponent());
        entity.setIcon(dto.getIcon());
        entity.setPermission(dto.getPermission());
        entity.setSort(dto.getSort() != null ? dto.getSort() : 0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setVisible(Boolean.TRUE.equals(dto.getVisible()) || dto.getVisible() == null ? 1 : 0);
        entity.setKeepAlive(Boolean.TRUE.equals(dto.getKeepAlive()) ? 1 : 0);
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sysMenuDao.insert(entity);
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MenuUpdateDTO dto) {
        SysMenuEntity entity = sysMenuDao.findById(id);
        if (entity == null) {
            throw new BusinessException("菜单不存在");
        }
        if (dto.getParentId() != null) {
            entity.setParentId(dto.getParentId());
        }
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getType() != null) {
            entity.setType(dto.getType());
        }
        if (dto.getPath() != null) {
            entity.setPath(dto.getPath());
        }
        if (dto.getComponent() != null) {
            entity.setComponent(dto.getComponent());
        }
        if (dto.getIcon() != null) {
            entity.setIcon(dto.getIcon());
        }
        if (dto.getPermission() != null) {
            entity.setPermission(dto.getPermission());
        }
        if (dto.getSort() != null) {
            entity.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getVisible() != null) {
            entity.setVisible(Boolean.TRUE.equals(dto.getVisible()) ? 1 : 0);
        }
        if (dto.getKeepAlive() != null) {
            entity.setKeepAlive(Boolean.TRUE.equals(dto.getKeepAlive()) ? 1 : 0);
        }
        entity.setUpdateTime(LocalDateTime.now());
        sysMenuDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysMenuEntity entity = sysMenuDao.findById(id);
        if (entity == null) {
            throw new BusinessException("菜单不存在");
        }
        if (sysMenuDao.countActiveChildren(id) > 0) {
            throw new BusinessException("存在子菜单，请先删除下级菜单");
        }
        sysMenuDao.softDeleteById(id);
    }

    private SysMenuVO toVo(SysMenuEntity entity) {
        SysMenuVO vo = new SysMenuVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setPath(entity.getPath());
        vo.setComponent(entity.getComponent());
        vo.setIcon(entity.getIcon());
        vo.setPermission(entity.getPermission());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setVisible(entity.getVisible() != null && entity.getVisible() == 1);
        vo.setKeepAlive(entity.getKeepAlive() != null && entity.getKeepAlive() == 1);
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
