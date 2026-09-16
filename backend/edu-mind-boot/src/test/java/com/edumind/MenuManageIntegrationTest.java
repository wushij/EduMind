package com.edumind;

import com.edumind.common.exception.BusinessException;
import com.edumind.system.dao.SysMenuDao;
import com.edumind.system.dto.menu.MenuCreateDTO;
import com.edumind.system.service.menu.SysMenuService;
import com.edumind.system.vo.menu.SysMenuVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
class MenuManageIntegrationTest {

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private SysMenuDao sysMenuDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void ensureTableAndClean() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_menu (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    parent_id BIGINT NOT NULL DEFAULT 0,
                    name VARCHAR(100) NOT NULL,
                    type TINYINT NOT NULL,
                    path VARCHAR(255),
                    component VARCHAR(255),
                    icon VARCHAR(64),
                    permission VARCHAR(128),
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    visible TINYINT NOT NULL DEFAULT 1,
                    keep_alive TINYINT NOT NULL DEFAULT 0,
                    deleted TINYINT NOT NULL DEFAULT 0,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        sysMenuDao.deleteAllForTest();
    }

    @Test
    @DisplayName("菜单增删改查树与子节点删除拦截")
    void menuCrudAndDeleteRules() {
        MenuCreateDTO rootDto = new MenuCreateDTO();
        rootDto.setParentId(0L);
        rootDto.setName("系统管理");
        rootDto.setType(1);
        rootDto.setSort(1);
        rootDto.setStatus(1);
        SysMenuVO root = sysMenuService.create(rootDto);

        MenuCreateDTO childDto = new MenuCreateDTO();
        childDto.setParentId(root.getId());
        childDto.setName("菜单管理");
        childDto.setType(2);
        childDto.setPath("/system/menu");
        childDto.setSort(1);
        childDto.setStatus(1);
        SysMenuVO child = sysMenuService.create(childDto);

        List<SysMenuVO> tree = sysMenuService.getTree(null);
        Assertions.assertEquals(1, tree.size());
        Assertions.assertEquals(1, tree.get(0).getChildren().size());

        Assertions.assertThrows(BusinessException.class, () -> sysMenuService.delete(root.getId()));

        sysMenuService.delete(child.getId());
        Assertions.assertNull(sysMenuDao.findById(child.getId()));

        sysMenuService.delete(root.getId());
        Assertions.assertNull(sysMenuDao.findById(root.getId()));
    }
}
