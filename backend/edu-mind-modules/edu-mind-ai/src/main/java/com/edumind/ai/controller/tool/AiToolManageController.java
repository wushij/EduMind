package com.edumind.ai.controller.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.ai.dto.tool.AiToolFlagsDTO;
import com.edumind.ai.dto.tool.AiToolSaveDTO;
import com.edumind.ai.dto.tool.AiToolUpdateDTO;
import com.edumind.ai.service.tool.AiToolManageService;
import com.edumind.ai.vo.tool.AiToolAdminVO;
import com.edumind.ai.vo.tool.AiToolStatsVO;
import com.edumind.common.annotation.OperationLog;
import com.edumind.common.api.ApiResult;
import com.edumind.common.enums.BusinessType;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/system/ai-tools", "/api/system/tools"})
@RequiredArgsConstructor
public class AiToolManageController {

    private final AiToolManageService aiToolManageService;

    @SaCheckPermission("system:tool:view")
    @GetMapping
    public ApiResult<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isRecommended) {
        List<AiToolAdminVO> list = aiToolManageService.list(category, status, keyword, isRecommended);
        Map<String, Object> page = new HashMap<>();
        page.put("records", list);
        page.put("list", list);
        page.put("total", list.size());
        return ApiResult.success(page);
    }

    @SaCheckPermission("system:tool:view")
    @GetMapping("/stats")
    public ApiResult<AiToolStatsVO> stats() {
        return ApiResult.success(aiToolManageService.stats());
    }

    @SaCheckPermission("system:tool:view")
    @GetMapping("/{id}")
    public ApiResult<AiToolAdminVO> getById(@PathVariable String id) {
        return ApiResult.success(aiToolManageService.getById(id));
    }

    @SaCheckPermission("system:tool:edit")
    @PostMapping
    @OperationLog(module = "AI教学工具", title = "新增AI工具", businessType = BusinessType.INSERT)
    public ApiResult<Map<String, Object>> create(@Valid @RequestBody AiToolSaveDTO dto) {
        String id = aiToolManageService.create(dto);
        return ApiResult.success(Map.of("id", id));
    }

    @SaCheckPermission("system:tool:edit")
    @PutMapping("/{id}")
    @OperationLog(module = "AI教学工具", title = "修改AI工具", businessType = BusinessType.UPDATE)
    public ApiResult<Void> update(@PathVariable String id, @Valid @RequestBody AiToolUpdateDTO dto) {
        aiToolManageService.update(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("system:tool:edit")
    @PostMapping("/{id}/publish")
    @OperationLog(module = "AI教学工具", title = "上架AI工具", businessType = BusinessType.GRANT)
    public ApiResult<Void> publish(@PathVariable String id) {
        aiToolManageService.publish(id);
        return ApiResult.success();
    }

    @SaCheckPermission("system:tool:edit")
    @PostMapping("/{id}/offline")
    @OperationLog(module = "AI教学工具", title = "下架AI工具", businessType = BusinessType.UPDATE)
    public ApiResult<Void> offline(@PathVariable String id) {
        aiToolManageService.offline(id);
        return ApiResult.success();
    }

    @SaCheckPermission("system:tool:edit")
    @PostMapping("/{id}/flags")
    @OperationLog(module = "AI教学工具", title = "更新AI工具标记", businessType = BusinessType.UPDATE)
    public ApiResult<Void> updateFlags(@PathVariable String id, @RequestBody AiToolFlagsDTO dto) {
        aiToolManageService.updateFlags(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("system:tool:edit")
    @DeleteMapping("/{id}")
    @OperationLog(module = "AI教学工具", title = "删除AI工具", businessType = BusinessType.DELETE)
    public ApiResult<Void> delete(@PathVariable String id) {
        aiToolManageService.delete(id);
        return ApiResult.success();
    }
}
