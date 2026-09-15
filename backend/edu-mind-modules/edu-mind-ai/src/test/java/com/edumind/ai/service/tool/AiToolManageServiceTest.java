package com.edumind.ai.service.tool;

import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiModelConfigDao;
import com.edumind.ai.dao.AiToolDao;
import com.edumind.ai.dto.tool.AiToolSaveDTO;
import com.edumind.ai.dto.tool.AiToolUpdateDTO;
import com.edumind.ai.entity.AiToolEntity;
import com.edumind.ai.service.tool.impl.AiToolManageServiceImpl;
import com.edumind.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiToolManageServiceTest {

    @Mock
    private AiToolDao aiToolDao;

    @Mock
    private AiModelConfigDao aiModelConfigDao;

    @Spy
    private AiConverter aiConverter = new AiConverter();

    @InjectMocks
    private AiToolManageServiceImpl aiToolManageService;

    private AiToolEntity existingTool;

    @BeforeEach
    void setUp() {
        existingTool = new AiToolEntity();
        existingTool.setId("tool_custom_demo");
        existingTool.setName("自定义工具");
        existingTool.setCategory("GENERAL");
        existingTool.setRoute("/ai/demo");
        existingTool.setExecutionMode("ROUTE");
        existingTool.setStatus(1);
        existingTool.setUseCount(10);
        existingTool.setIsRecommended(0);
        existingTool.setIsHot(0);
        existingTool.setSortOrder(0);
    }

    @Test
    void create_shouldInsertNewTool() {
        AiToolSaveDTO dto = new AiToolSaveDTO();
        dto.setId("tool_custom_new");
        dto.setName("新工具");
        dto.setCategory("TEACHER");
        dto.setRoute("/ai/new");
        dto.setExecutionMode("ROUTE");

        when(aiToolDao.existsById("tool_custom_new")).thenReturn(false);

        String id = aiToolManageService.create(dto);

        assertEquals("tool_custom_new", id);
        verify(aiToolDao).insert(any(AiToolEntity.class));
    }

    @Test
    void create_shouldRejectDuplicateId() {
        AiToolSaveDTO dto = new AiToolSaveDTO();
        dto.setId("tool_custom_demo");
        dto.setName("重复");
        dto.setCategory("GENERAL");
        dto.setRoute("/ai/demo");
        dto.setExecutionMode("ROUTE");

        when(aiToolDao.existsById("tool_custom_demo")).thenReturn(true);

        assertThrows(BusinessException.class, () -> aiToolManageService.create(dto));
    }

    @Test
    void offline_shouldHideFromUserList() {
        when(aiToolDao.findById("tool_custom_demo")).thenReturn(existingTool);

        aiToolManageService.offline("tool_custom_demo");

        assertEquals(0, existingTool.getStatus());
        verify(aiToolDao).update(existingTool);
    }

    @Test
    void publish_shouldRestoreOnlineStatus() {
        existingTool.setStatus(0);
        when(aiToolDao.findById("tool_custom_demo")).thenReturn(existingTool);

        aiToolManageService.publish("tool_custom_demo");

        assertEquals(1, existingTool.getStatus());
        verify(aiToolDao).update(existingTool);
    }

    @Test
    void delete_shouldRejectSeedTool() {
        assertThrows(BusinessException.class, () -> aiToolManageService.delete("tool_question_gen"));
        verify(aiToolDao, never()).deleteById(any());
    }

    @Test
    void delete_shouldRemoveCustomTool() {
        when(aiToolDao.existsById("tool_custom_demo")).thenReturn(true);

        aiToolManageService.delete("tool_custom_demo");

        verify(aiToolDao).deleteById("tool_custom_demo");
    }

    @Test
    void update_shouldApplyChanges() {
        AiToolUpdateDTO dto = new AiToolUpdateDTO();
        dto.setName("更新后名称");
        dto.setCategory("STUDENT");
        dto.setRoute("/learning/demo");
        dto.setExecutionMode("ROUTE");

        when(aiToolDao.findById("tool_custom_demo")).thenReturn(existingTool);

        aiToolManageService.update("tool_custom_demo", dto);

        assertEquals("更新后名称", existingTool.getName());
        assertEquals("STUDENT", existingTool.getCategory());
        verify(aiToolDao).update(existingTool);
    }

    @Test
    void list_shouldReturnAdminView() {
        when(aiToolDao.listForAdmin(null, null, null, null)).thenReturn(List.of(existingTool));

        var result = aiToolManageService.list(null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("tool_custom_demo", result.get(0).getId());
    }
}
