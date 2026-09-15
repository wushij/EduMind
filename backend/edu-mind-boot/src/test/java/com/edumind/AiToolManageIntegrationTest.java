package com.edumind;

import com.edumind.ai.dao.AiToolDao;
import com.edumind.ai.dto.tool.AiToolSaveDTO;
import com.edumind.ai.dto.tool.AiToolUpdateDTO;
import com.edumind.ai.service.tool.AiToolManageService;
import com.edumind.ai.service.tool.AiToolService;
import com.edumind.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V2.1 · AI 教学工具管理端集成测试（管理 CRUD + 广场可见性 + 调用计数）
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
class AiToolManageIntegrationTest {

    private static final String TEST_TOOL_ID = "tool_gate_v21_demo";

    @Autowired
    private AiToolManageService aiToolManageService;

    @Autowired
    private AiToolService aiToolService;

    @Autowired
    private AiToolDao aiToolDao;

    @AfterEach
    void cleanup() {
        if (aiToolDao.existsById(TEST_TOOL_ID)) {
            aiToolDao.deleteById(TEST_TOOL_ID);
        }
    }

    @Test
    @DisplayName("V2.1-1 创建自定义工具 → 上架 → 广场可见 → 计数 → 下架 → 删除")
    void manageLifecycle_shouldSyncMarketplaceVisibility() {
        AiToolSaveDTO createDto = new AiToolSaveDTO();
        createDto.setId(TEST_TOOL_ID);
        createDto.setName("Gate V2.1 演示工具");
        createDto.setDescription("集成测试工具");
        createDto.setCategory("GENERAL");
        createDto.setRoute("/ai/marketplace");
        createDto.setExecutionMode("ROUTE");
        createDto.setSortOrder(5);

        String id = aiToolManageService.create(createDto);
        assertEquals(TEST_TOOL_ID, id);

        aiToolManageService.publish(TEST_TOOL_ID);
        assertTrue(aiToolService.listTools("ALL", "Gate V2.1").stream()
                .anyMatch(tool -> TEST_TOOL_ID.equals(tool.getId())));

        aiToolService.recordToolUse(TEST_TOOL_ID);
        assertTrue(aiToolManageService.getById(TEST_TOOL_ID).getUseCount() >= 1);

        aiToolManageService.offline(TEST_TOOL_ID);
        assertTrue(aiToolService.listTools("ALL", "Gate V2.1").stream()
                .noneMatch(tool -> TEST_TOOL_ID.equals(tool.getId())));

        aiToolManageService.delete(TEST_TOOL_ID);
        assertThrows(BusinessException.class, () -> aiToolManageService.getById(TEST_TOOL_ID));
    }

    @Test
    @DisplayName("V2.1-2 内置种子工具不可删除")
    void delete_shouldRejectSeedTool() {
        assertThrows(BusinessException.class, () -> aiToolManageService.delete("tool_question_gen"));
    }

    @Test
    @DisplayName("V2.1-3 管理端统计与列表可查")
    void statsAndList_shouldReturnData() {
        assertTrue(aiToolManageService.stats().getTotal() >= 1);
        assertTrue(aiToolManageService.list(null, 1, null, null).size() >= 1);
    }

    @Test
    @DisplayName("V2.1-4 更新工具元数据持久化")
    void update_shouldPersistMetadata() {
        AiToolSaveDTO createDto = new AiToolSaveDTO();
        createDto.setId(TEST_TOOL_ID);
        createDto.setName("待更新工具");
        createDto.setCategory("TEACHER");
        createDto.setRoute("/ai/question/generate");
        createDto.setExecutionMode("ROUTE");
        aiToolManageService.create(createDto);

        AiToolUpdateDTO updateDto = new AiToolUpdateDTO();
        updateDto.setName("已更新工具");
        updateDto.setCategory("TEACHER");
        updateDto.setRoute("/ai/question/generate");
        updateDto.setExecutionMode("ROUTE");
        updateDto.setSortOrder(99);
        updateDto.setIsRecommended(true);
        aiToolManageService.update(TEST_TOOL_ID, updateDto);

        var vo = aiToolManageService.getById(TEST_TOOL_ID);
        assertEquals("已更新工具", vo.getName());
        assertEquals(99, vo.getSortOrder());
        assertTrue(vo.getIsRecommended());
    }
}
