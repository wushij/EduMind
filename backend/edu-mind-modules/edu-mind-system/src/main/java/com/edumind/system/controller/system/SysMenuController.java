package com.edumind.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.menu.MenuCreateDTO;
import com.edumind.system.dto.menu.MenuUpdateDTO;
import com.edumind.system.service.menu.SysMenuService;
import com.edumind.system.vo.menu.SysMenuVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @SaCheckPermission(value = {"system:menu:view", "system:menu:edit"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @GetMapping("/tree")
    public ApiResult<List<SysMenuVO>> getTree(@RequestParam(required = false) String keyword) {
        return ApiResult.success(sysMenuService.getTree(keyword));
    }

    @SaCheckPermission("system:menu:edit")
    @PostMapping
    public ApiResult<SysMenuVO> create(@Valid @RequestBody MenuCreateDTO dto) {
        return ApiResult.success(sysMenuService.create(dto));
    }

    @SaCheckPermission("system:menu:edit")
    @PutMapping("/{id}")
    public ApiResult<Boolean> update(@PathVariable("id") Long id, @RequestBody MenuUpdateDTO dto) {
        sysMenuService.update(id, dto);
        return ApiResult.success(true);
    }

    @SaCheckPermission("system:menu:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable("id") Long id) {
        sysMenuService.delete(id);
        return ApiResult.success(true);
    }
}
