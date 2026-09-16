package com.edumind.system.service.menu;

import com.edumind.system.dto.menu.MenuCreateDTO;
import com.edumind.system.dto.menu.MenuUpdateDTO;
import com.edumind.system.vo.menu.SysMenuVO;

import java.util.List;

public interface SysMenuService {

    List<SysMenuVO> getTree(String keyword);

    SysMenuVO create(MenuCreateDTO dto);

    void update(Long id, MenuUpdateDTO dto);

    void delete(Long id);
}
